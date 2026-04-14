package com.acedigital.meal_plan_manager.recipe;

import java.net.URI;
import java.util.List;

import com.acedigital.meal_plan_manager.recipe.dto.CreateRecipeRequest;
import com.acedigital.meal_plan_manager.recipe.dto.RecipeResponse;
import com.acedigital.meal_plan_manager.recipe.dto.SaveRecipeRequest;
import com.acedigital.meal_plan_manager.recipe.dto.ShareRecipeRequest;
import com.acedigital.meal_plan_manager.recipe.dto.UpdateRecipeRequest;
import com.acedigital.meal_plan_manager.user.User;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

  private final RecipeService recipeService;

  public RecipeController(RecipeService recipeService) {
    this.recipeService = recipeService;
  }

  @GetMapping
  public ResponseEntity<List<RecipeResponse>> getMyRecipes(@AuthenticationPrincipal User currentUser) {
    List<RecipeResponse> body = recipeService.getRecipesFor(currentUser)
        .stream()
        .map(RecipeResponse::from)
        .toList();
    return ResponseEntity.ok(body);
  }

  @GetMapping("/{id}")
  public ResponseEntity<RecipeResponse> getRecipeById(
      @PathVariable Long id,
      @AuthenticationPrincipal User currentUser) {
    Recipe recipe = recipeService.getRecipeFor(id, currentUser);
    return ResponseEntity.ok(RecipeResponse.from(recipe));
  }

  @PostMapping
  public ResponseEntity<RecipeResponse> createRecipe(
      @Valid @RequestBody CreateRecipeRequest request,
      @AuthenticationPrincipal User currentUser) {
    Recipe saved = recipeService.createRecipe(request, currentUser);
    return ResponseEntity
        .created(URI.create("/api/recipes/" + saved.getId()))
        .body(RecipeResponse.from(saved));
  }

  /**
   * Bookmark an existing recipe for the current user. Returns 201 with the
   * saved row's id. Idempotent — saving the same recipe twice is a no-op.
   */
  @PostMapping("/save")
  public ResponseEntity<SavedRecipe> saveRecipe(
      @Valid @RequestBody SaveRecipeRequest request,
      @AuthenticationPrincipal User currentUser) {
    SavedRecipe saved = recipeService.saveRecipe(request.recipeId(), currentUser);
    return ResponseEntity
        .created(URI.create("/api/recipes/" + request.recipeId()))
        .body(saved);
  }

  /**
   * Share an owned recipe with another user. Returns 201 on success, 404 if
   * the recipe doesn't exist or isn't owned by the caller, 404 if the target
   * username doesn't exist, 400 if the caller tries to share with themselves.
   */
  @PostMapping("/share")
  public ResponseEntity<SharedRecipe> shareRecipe(
      @Valid @RequestBody ShareRecipeRequest request,
      @AuthenticationPrincipal User currentUser) {
    SharedRecipe shared = recipeService.shareRecipe(
        request.recipeId(), request.shareWithUsername(), currentUser);
    return ResponseEntity
        .created(URI.create("/api/recipes/" + request.recipeId()))
        .body(shared);
  }

  @PutMapping("/{id}")
  public ResponseEntity<RecipeResponse> updateRecipe(
      @PathVariable Long id,
      @Valid @RequestBody UpdateRecipeRequest request,
      @AuthenticationPrincipal User currentUser) {
    Recipe updated = recipeService.updateRecipe(id, request, currentUser);
    return ResponseEntity.ok(RecipeResponse.from(updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> softDeleteRecipe(
      @PathVariable Long id,
      @AuthenticationPrincipal User currentUser) {
    recipeService.softDelete(id, currentUser);
    return ResponseEntity.noContent().build();
  }
}
