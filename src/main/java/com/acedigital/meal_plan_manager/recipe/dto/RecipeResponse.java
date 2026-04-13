package com.acedigital.meal_plan_manager.recipe.dto;

import com.acedigital.meal_plan_manager.recipe.CuisineType;
import com.acedigital.meal_plan_manager.recipe.Difficulty;
import com.acedigital.meal_plan_manager.recipe.Recipe;

/**
 * Output DTO — explicit projection over {@link Recipe}. Keeps server-side
 * metadata (user, deleted flags, lazy relations) out of JSON responses.
 */
public record RecipeResponse(
    Long id,
    Long userId,
    String title,
    String description,
    String instructions,
    Integer prepTime,
    Integer cookTime,
    Integer servings,
    Difficulty difficulty,
    CuisineType cuisineType,
    String imageUrl,
    Boolean isPublic,
    Double averageRating,
    Integer totalReviews) {

  public static RecipeResponse from(Recipe recipe) {
    return new RecipeResponse(
        recipe.getId(),
        recipe.getUserId(),
        recipe.getTitle(),
        recipe.getDescription(),
        recipe.getInstructions(),
        recipe.getPrepTime(),
        recipe.getCookTime(),
        recipe.getServings(),
        recipe.getDifficulty(),
        recipe.getCuisineType(),
        recipe.getImageUrl(),
        recipe.getIsPublic(),
        recipe.getAverageRating(),
        recipe.getTotalReviews());
  }
}
