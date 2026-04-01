package com.acedigital.meal_plan_manager.recipe;

import java.util.List;

import org.springframework.http.ResponseEntity;

public class RecipeService {

  private RecipeRepository recipeRepository;

  public RecipeService(RecipeRepository recipeRepository) {
    this.recipeRepository = recipeRepository;
  }

  public ResponseEntity<List<Recipe>> getAllRecipes() {
    List<Recipe> recipes = recipeRepository.findAll();
    return ResponseEntity.ok(recipes);
  }
}
