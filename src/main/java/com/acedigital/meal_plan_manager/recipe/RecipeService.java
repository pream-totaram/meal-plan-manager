package com.acedigital.meal_plan_manager.recipe;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.acedigital.meal_plan_manager.recipe.dto.CreateRecipeRequest;
import com.acedigital.meal_plan_manager.recipe.dto.UpdateRecipeRequest;
import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RecipeService {

  private final RecipeRepository recipeRepository;
  private final SavedRecipeRepository savedRecipeRepository;
  private final SharedRecipeRepository sharedRecipeRepository;
  private final UserRepository userRepository;

  public RecipeService(RecipeRepository recipeRepository,
      SavedRecipeRepository savedRecipeRepository,
      SharedRecipeRepository sharedRecipeRepository,
      UserRepository userRepository) {
    this.recipeRepository = recipeRepository;
    this.savedRecipeRepository = savedRecipeRepository;
    this.sharedRecipeRepository = sharedRecipeRepository;
    this.userRepository = userRepository;
  }

  /**
   * Share an owned recipe with another user identified by username. Only the
   * recipe's owner may share it. Sharing with a non-existent user, or with
   * yourself, is rejected. Idempotent: re-sharing the same recipe with the
   * same recipient returns a representative row without inserting a
   * duplicate.
   */
  public SharedRecipe shareRecipe(Long recipeId, String shareWithUsername, User sharer) {
    Recipe recipe = recipeRepository.findById(recipeId)
        .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    boolean owned = recipe.getUser() != null
        && recipe.getUser().getId().equals(sharer.getId());
    if (!owned) {
      // Only the owner may share — same-shape 404 to avoid leaking ownership.
      throw new EntityNotFoundException("Recipe not found");
    }

    User target = userRepository.findByUsername(shareWithUsername)
        .orElseThrow(() -> new EntityNotFoundException("Target user not found"));

    if (target.getId().equals(sharer.getId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "Cannot share a recipe with yourself");
    }

    if (sharedRecipeRepository.existsByRecipe_IdAndSharedWith_Id(recipeId, target.getId())) {
      return SharedRecipe.builder()
          .recipe(recipe)
          .sharedBy(sharer)
          .sharedWith(target)
          .sharedAt(LocalDateTime.now())
          .build();
    }

    SharedRecipe share = SharedRecipe.builder()
        .recipe(recipe)
        .sharedBy(sharer)
        .sharedWith(target)
        .sharedAt(LocalDateTime.now())
        .build();
    return sharedRecipeRepository.save(share);
  }

  /**
   * Bookmark an existing recipe for {@code user}. The recipe must be either
   * public or owned by {@code user} — attempting to save a private recipe
   * that belongs to someone else is treated identically to "not found" so
   * callers cannot probe for the existence of private recipes.
   *
   * Idempotent: a repeat call with the same (user, recipe) returns the
   * existing {@link SavedRecipe} row rather than inserting a duplicate.
   */
  public SavedRecipe saveRecipe(Long recipeId, User user) {
    Recipe recipe = recipeRepository.findById(recipeId)
        .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    boolean owned = recipe.getUser() != null
        && recipe.getUser().getId().equals(user.getId());
    boolean isPublic = Boolean.TRUE.equals(recipe.getIsPublic());
    if (!owned && !isPublic) {
      // Do not reveal that the recipe exists.
      throw new EntityNotFoundException("Recipe not found");
    }

    if (savedRecipeRepository.existsByUser_IdAndRecipe_Id(user.getId(), recipeId)) {
      // Already bookmarked — rebuild a representative row without hitting the
      // DB again. Callers treat this as a no-op success.
      return SavedRecipe.builder()
          .user(user)
          .recipe(recipe)
          .savedAt(LocalDateTime.now())
          .build();
    }

    SavedRecipe saved = SavedRecipe.builder()
        .user(user)
        .recipe(recipe)
        .savedAt(LocalDateTime.now())
        .build();
    return savedRecipeRepository.save(saved);
  }

  public List<Recipe> getRecipesFor(User owner) {
    return recipeRepository.findAllByUser_Id(owner.getId());
  }

  public Recipe getRecipeFor(Long id, User owner) {
    return recipeRepository.findByIdAndUser_Id(id, owner.getId())
        .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));
  }

  public Recipe createRecipe(CreateRecipeRequest request, User owner) {
    Recipe recipe = Recipe.builder()
        .title(request.title())
        .description(request.description())
        .instructions(request.instructions())
        .prepTime(request.prepTime())
        .cookTime(request.cookTime())
        .servings(request.servings())
        .difficulty(request.difficulty())
        .cuisineType(request.cuisineType())
        .imageUrl(request.imageUrl())
        .isPublic(request.isPublic())
        // Server-controlled fields. Never populated from the request.
        .averageRating(0.0)
        .totalReviews(0)
        .user(owner)
        .build();
    return recipeRepository.save(recipe);
  }

  public Recipe updateRecipe(Long id, UpdateRecipeRequest request, User owner) {
    Recipe recipe = recipeRepository.findByIdAndUser_Id(id, owner.getId())
        .orElseThrow(() -> new EntityNotFoundException("Recipe not found"));

    recipe.setTitle(request.title());
    recipe.setDescription(request.description());
    recipe.setInstructions(request.instructions());
    recipe.setPrepTime(request.prepTime());
    recipe.setCookTime(request.cookTime());
    recipe.setServings(request.servings());
    recipe.setDifficulty(request.difficulty());
    recipe.setCuisineType(request.cuisineType());
    recipe.setImageUrl(request.imageUrl());
    recipe.setIsPublic(request.isPublic());
    // NOTE: averageRating and totalReviews are intentionally not mutable
    // via the update endpoint — they are computed server-side from reviews.

    return recipeRepository.save(recipe);
  }

  public void softDelete(Long id, User owner) {
    int updated = recipeRepository.softDeleteByIdAndUserId(id, owner.getId());
    if (updated == 0) {
      // Treat "not yours" and "doesn't exist" identically to avoid leaking
      // which recipe ids are in the database.
      throw new AccessDeniedException("Recipe not found");
    }
  }
}
