package com.acedigital.meal_plan_manager.recipe;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.EntityManager;

import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
public class RecipeFunctionalTest {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private RecipeRepository recipeRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  void setUp() {
    recipeRepository.deleteAll();
    userRepository.deleteAll();

    // Create and save a test user
    User user = new User();
    user.setUsername("somebody");
    user.setPassword(passwordEncoder.encode("password"));
    user.setEmail("somebody@mail.com");
    userRepository.save(user);

    // Create test recipes
    Recipe recipe1 = Recipe.builder().title("Spaghetti Carbonara").description("Classic Italian pasta dish")
        .prepTime(15).cookTime(20).user(user).build();

    Recipe recipe2 = Recipe.builder().title("Chicken Curry").description("Spicy Indian curry").prepTime(20)
        .cookTime(30).user(user).build();

    Recipe recipe3 = Recipe.builder().title("Caesar Salad").description("Fresh romaine lettuce with Caesar dressing")
        .prepTime(10).cookTime(0).user(user).build();

    Recipe deletedRecipe = Recipe.builder().title("Deleted Recipe")
        .description("This recipe should not appear in results").prepTime(5).cookTime(10).deleted(true)
        .deletedAt(LocalDateTime.now()).user(user).build();

    recipeRepository.saveAll(Arrays.asList(recipe1, recipe2, recipe3, deletedRecipe));
  }

  @Test
  @Disabled
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
  @Disabled
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

    ResponseEntity<String> response = restTemplate.withBasicAuth("somebody", "password")
        .postForEntity("/api/recipes",
            newRecipe, String.class);
    System.out.println(response.getBody());
    // Recipe createdRecipe = response.getBody();
    // assertTrue(createdRecipe.getTitle().equals("Tacos"));
    // assertTrue(createdRecipe.getId() > 4);
  }

  @Test
  @Disabled
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
  @Disabled
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
