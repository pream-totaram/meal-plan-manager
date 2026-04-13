package com.acedigital.meal_plan_manager.recipe;

import java.net.URI;
import java.util.List;

import com.acedigital.meal_plan_manager.recipe.dto.CreateRecipeRequest;
import com.acedigital.meal_plan_manager.recipe.dto.RecipeResponse;
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
