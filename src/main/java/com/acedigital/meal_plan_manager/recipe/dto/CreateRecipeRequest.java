package com.acedigital.meal_plan_manager.recipe.dto;

import com.acedigital.meal_plan_manager.recipe.CuisineType;
import com.acedigital.meal_plan_manager.recipe.Difficulty;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for POST /api/recipes. Explicitly does NOT expose
 * {@code user}, {@code id}, {@code deleted}, {@code deletedAt},
 * {@code averageRating}, or {@code totalReviews} — those are
 * server-controlled and must not be mass-assigned from the request body.
 */
public record CreateRecipeRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 5000) String description,
    @Size(max = 20000) String instructions,
    @Min(0) @Max(1440) Integer prepTime,
    @Min(0) @Max(1440) Integer cookTime,
    @Min(1) @Max(100) Integer servings,
    Difficulty difficulty,
    CuisineType cuisineType,
    // URL pointing to an image. A strict scheme check prevents
    // javascript:, data:, file: and similar dangerous schemes, and
    // stops path traversal via relative paths.
    @Size(max = 512) @Pattern(regexp = "^$|^https?://.+", message = "imageUrl must be http(s)://...") String imageUrl,
    Boolean isPublic) {
}
