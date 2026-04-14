package com.acedigital.meal_plan_manager.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedRecipeRepository extends JpaRepository<SharedRecipe, Long> {

  /**
   * Idempotency guard for the share endpoint — a repeat share to the same
   * recipient must be a no-op rather than violating
   * {@code uq_shared_recipes_recipe_target}.
   */
  boolean existsByRecipe_IdAndSharedWith_Id(Long recipeId, Long sharedWithUserId);
}
