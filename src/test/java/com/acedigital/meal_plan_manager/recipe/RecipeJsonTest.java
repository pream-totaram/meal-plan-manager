package com.acedigital.meal_plan_manager.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

@JsonTest
public class RecipeJsonTest {

  @Autowired
  private JacksonTester<Recipe> json;

  @Test
  public void recipeSerializationTest() throws IOException {
    Recipe recipe = Recipe.builder()
        .id(1L)
        .title("Test Recipe")
        .description("This is a test recipe.")
        .instructions("Instructions for making the test recipe.")
        .prepTime(15)
        .cookTime(30)
        .servings(4)
        .difficulty(Difficulty.EASY)
        .cuisineType(CuisineType.ITALIAN)
        .image(new File("test-image.jpg"))
        .isPublic(true)
        .averageRating(4.5)
        .totalReviews(10)
        .build();

    JsonContent<Recipe> jsonContent = json.write(recipe);
    // Add assertions for JSON content
    assertThat(jsonContent).extractingJsonPathNumberValue("$.id").isEqualTo(1);
    assertThat(jsonContent).extractingJsonPathStringValue("$.title").isEqualTo("Test Recipe");
    assertThat(jsonContent).extractingJsonPathStringValue("$.description").isEqualTo("This is a test recipe.");
    assertThat(jsonContent).extractingJsonPathStringValue("$.instructions")
        .isEqualTo("Instructions for making the test recipe.");
    assertThat(jsonContent).extractingJsonPathNumberValue("$.prepTime").isEqualTo(15);
    assertThat(jsonContent).extractingJsonPathNumberValue("$.cookTime").isEqualTo(30);
    assertThat(jsonContent).extractingJsonPathNumberValue("$.servings").isEqualTo(4);
    assertThat(jsonContent).extractingJsonPathStringValue("$.difficulty").isEqualTo(Difficulty.EASY.name());
    assertThat(jsonContent).extractingJsonPathStringValue("$.cuisineType").isEqualTo(CuisineType.ITALIAN.name());
    assertThat(jsonContent).extractingJsonPathStringValue("$.imagePath").contains("test-image.jpg");
    assertThat(jsonContent).extractingJsonPathBooleanValue("$.isPublic").isTrue();
    assertThat(jsonContent).extractingJsonPathNumberValue("$.averageRating").isEqualTo(4.5);
    assertThat(jsonContent).extractingJsonPathNumberValue("$.totalReviews").isEqualTo(10);
  }
}
