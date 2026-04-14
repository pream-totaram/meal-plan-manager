package com.acedigital.meal_plan_manager.recipe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ShareRecipeRequest(
    @NotNull Long recipeId,
    @NotBlank String shareWithUsername) {
}
