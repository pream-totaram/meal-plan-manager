package com.acedigital.meal_plan_manager.recipe;

import java.util.List;

import com.acedigital.meal_plan_manager.user.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {
  private final RecipeRepository recipeRepository;
  private final UserRepository userRepository;

  // Implement CRUD operations for Recipe entity
  @GetMapping
  public ResponseEntity<List<Recipe>> getAllRecipes() {
    List<Recipe> recipes = recipeRepository.findAll();
    return ResponseEntity.ok(recipes);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Recipe> getRecipeById(@PathVariable Long id) {
    return recipeRepository.findById(id)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<Recipe> createRecipe(@RequestBody Recipe recipe) {
    recipe.setUser(userRepository.findById(1L).orElseThrow(() -> new IllegalArgumentException("User not found")));
    return ResponseEntity.ok(recipeRepository.save(recipe));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe update) {
    return recipeRepository.findById(id)
        .map(recipe -> {
          recipe.applyUpdate(update);
          return ResponseEntity.ok(recipeRepository.save(recipe));
        })
        .orElse(ResponseEntity.notFound().build());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> softDeleteRecipe(@PathVariable Long id) {
    recipeRepository.softDelete(id);
    return ResponseEntity.ok().build();
  }
}
