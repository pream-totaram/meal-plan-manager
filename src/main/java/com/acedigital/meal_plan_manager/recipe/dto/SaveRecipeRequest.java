package com.acedigital.meal_plan_manager.recipe.dto;

import jakarta.validation.constraints.NotNull;

public record SaveRecipeRequest(@NotNull Long recipeId) {
}
