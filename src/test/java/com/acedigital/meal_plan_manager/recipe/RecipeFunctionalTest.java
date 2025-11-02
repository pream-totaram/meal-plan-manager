package com.acedigital.meal_plan_manager.recipe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Sql("recipes.sql")
public class RecipeFunctionalTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private RecipeRepository recipeRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  public void shouldReturnAllRecipesWhenGetIsCalled() {
    ResponseEntity<List<Recipe>> response = restTemplate.exchange("/api/recipes",
        HttpMethod.GET, null,
        new ParameterizedTypeReference<List<Recipe>>() {
        });
    assertTrue(response.getStatusCode().is2xxSuccessful());
    assertTrue(response.getBody().size() == 4);
    List<Recipe> recipes = response.getBody();
    assertTrue(recipes.stream().anyMatch(recipe -> recipe.getTitle().equals("Spaghetti Carbonara")));
  }

  @Test
  public void shouldReturnCorrectRecipeDetailsWhenGetByIdIsCalled() {
    ResponseEntity<Recipe> response = restTemplate.getForEntity("/api/recipes/1",
        Recipe.class);
    assertTrue(response.getStatusCode().is2xxSuccessful());
    Recipe recipe = response.getBody();
    assertTrue(recipe.getTitle().equals("Spaghetti Carbonara"));
    assertTrue(Objects.isNull(recipe.getInstructions()));
    assertTrue(Objects.isNull(recipe.getServings()));
    assertTrue(recipe.getPrepTime() == 15);
    assertTrue(recipe.getCookTime() == 20);
  }

  @Test
  public void shouldCreateNewRecipeWhenPostIsCalled() throws JsonProcessingException {
    Recipe newRecipe = Recipe.builder()
        .title("Tacos")
        .description("Marlin tacos from the Mexican coast.")
        .instructions("Put stuff into tortillas.")
        .prepTime(10)
        .cookTime(25)
        .servings(3)
        .difficulty(Difficulty.EASY)
        .cuisineType(CuisineType.MEXICAN)
        .image(new File("new-test-image.jpg"))
        .isPublic(false)
        .averageRating(3.8)
        .build();

    ResponseEntity<Recipe> response = restTemplate.postForEntity("/api/recipes",
        newRecipe, Recipe.class);
    Recipe createdRecipe = response.getBody();
    assertTrue(createdRecipe.getTitle().equals("Tacos"));
    assertTrue(createdRecipe.getId() > 4);
  }

  @Test
  public void shouldUpdateRecipeWhenPutIsCalled() {
    Recipe update = Recipe.builder()
        .title("Updated Spaghetti Carbonara")
        .description("Updated version of the spaghetti carbonara recipe.")
        .instructions("Instructions for making the updated spaghetti carbonara.")
        .prepTime(10)
        .cookTime(20)
        .servings(4)
        .difficulty(Difficulty.EASY)
        .cuisineType(CuisineType.ITALIAN)
        .image(new File("updated-test-image.jpg"))
        .isPublic(true)
        .averageRating(4.2)
        .build();

    restTemplate.put("/api/recipes/1", update);

    ResponseEntity<Recipe> response = restTemplate.getForEntity("/api/recipes/1",
        Recipe.class);
    assertTrue(response.getStatusCode().is2xxSuccessful());
    Recipe updatedRecipe = response.getBody();
    assertTrue(updatedRecipe.getTitle().equals(update.getTitle()));
  }

  @Test
  public void shouldSoftDeleteRecipeWhenDeleteIsCalled() {
    Recipe recipe = recipeRepository.findById(100L).orElse(null);
    assertTrue(recipe != null);
    assertTrue(!recipe.getDeleted());
    restTemplate.delete("/api/recipes/100");
    assertTrue(recipeRepository.findById(100L).isEmpty());

    entityManager.clear();
    Recipe deletedRecipe = (Recipe) entityManager
        .createNativeQuery("SELECT * FROM recipes WHERE id = 100", Recipe.class)
        .getSingleResult();

    assertTrue(deletedRecipe.getDeleted());
  }

}
