package com.acedigital.meal_plan_manager.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {

  /**
   * Idempotency guard for the save endpoint — we check this before inserting
   * so a repeat POST returns the existing row rather than violating the
   * {@code uq_saved_recipes_user_recipe} unique constraint.
   */
  boolean existsByUser_IdAndRecipe_Id(Long userId, Long recipeId);
}
