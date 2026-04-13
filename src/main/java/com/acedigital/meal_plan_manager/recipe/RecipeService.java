package com.acedigital.meal_plan_manager.recipe;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.acedigital.meal_plan_manager.recipe.dto.CreateRecipeRequest;
import com.acedigital.meal_plan_manager.recipe.dto.UpdateRecipeRequest;
import com.acedigital.meal_plan_manager.user.User;

import jakarta.persistence.EntityNotFoundException;

@Service
public class RecipeService {

  private final RecipeRepository recipeRepository;

  public RecipeService(RecipeRepository recipeRepository) {
    this.recipeRepository = recipeRepository;
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
