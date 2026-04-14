package com.acedigital.meal_plan_manager.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Functional coverage for the CRUD half of {@link RecipeController}:
 * GET (list + by-id), POST create, PUT update, DELETE soft-delete. The
 * /save and /share endpoints have their own dedicated test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RecipeFunctionalTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RecipeRepository recipeRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private EntityManager entityManager;

  private User owner;
  private User otherUser;
  private Recipe recipe1;
  private Recipe recipe2;

  @BeforeEach
  void setUp() {
    owner = persistUser("owner");
    otherUser = persistUser("other");

    recipe1 = recipeRepository.save(Recipe.builder()
        .title("Spaghetti Carbonara").description("Classic Italian")
        .prepTime(15).cookTime(20).user(owner).build());
    recipe2 = recipeRepository.save(Recipe.builder()
        .title("Chicken Curry").description("Spicy")
        .prepTime(20).cookTime(30).user(owner).build());
    // Belongs to someone else — must never appear in owner's responses.
    recipeRepository.save(Recipe.builder()
        .title("Stranger Food").user(otherUser).build());
  }

  private User persistUser(String prefix) {
    User u = new User();
    u.setUsername(prefix + "-" + System.nanoTime());
    u.setEmail(u.getUsername() + "@example.com");
    u.setPassword(passwordEncoder.encode("password"));
    return userRepository.save(u);
  }

  // ---- GET /api/recipes ----------------------------------------------------

  @Test
  void getMyRecipes_returnsOnlyOwnedRecipes() throws Exception {
    mockMvc.perform(get("/api/recipes").with(user(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].title",
            Matchers.containsInAnyOrder("Spaghetti Carbonara", "Chicken Curry")));
  }

  @Test
  void getMyRecipes_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/recipes"))
        .andExpect(status().isUnauthorized());
  }

  // ---- GET /api/recipes/{id} ----------------------------------------------

  @Test
  void getRecipeById_returnsRecipeOwnedByCaller() throws Exception {
    mockMvc.perform(get("/api/recipes/" + recipe1.getId()).with(user(owner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Spaghetti Carbonara"))
        .andExpect(jsonPath("$.prepTime").value(15));
  }

  @Test
  void getRecipeById_returnsNotFoundForRecipeOwnedByAnother() throws Exception {
    mockMvc.perform(get("/api/recipes/" + recipe1.getId()).with(user(otherUser)))
        .andExpect(status().isNotFound());
  }

  @Test
  void getRecipeById_returnsNotFoundForUnknownId() throws Exception {
    mockMvc.perform(get("/api/recipes/999999").with(user(owner)))
        .andExpect(status().isNotFound());
  }

  // ---- POST /api/recipes ---------------------------------------------------

  @Test
  void createRecipe_persistsAndReturns201WithLocation() throws Exception {
    String body = """
        {"title":"Tacos","description":"Marlin tacos","instructions":"Assemble.",
         "prepTime":10,"cookTime":25,"servings":3,
         "difficulty":"EASY","cuisineType":"MEXICAN",
         "imageUrl":"https://example.com/img.jpg","isPublic":false}
        """;

    mockMvc.perform(post("/api/recipes")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", Matchers.containsString("/api/recipes/")))
        .andExpect(jsonPath("$.title").value("Tacos"))
        // averageRating / totalReviews must be server-controlled, not echoed
        // from the (absent) request body.
        .andExpect(jsonPath("$.averageRating").value(0.0))
        .andExpect(jsonPath("$.totalReviews").value(0));
  }

  @Test
  void createRecipe_rejectsBlankTitle() throws Exception {
    String body = "{\"title\":\"\"}";
    mockMvc.perform(post("/api/recipes")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createRecipe_rejectsNonHttpImageUrl() throws Exception {
    String body = """
        {"title":"x","imageUrl":"javascript:alert(1)"}
        """;
    mockMvc.perform(post("/api/recipes")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createRecipe_requiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/recipes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"x\"}"))
        .andExpect(status().isUnauthorized());
  }

  // ---- PUT /api/recipes/{id} -----------------------------------------------

  @Test
  void updateRecipe_modifiesOwnedRecipe() throws Exception {
    String body = """
        {"title":"Updated","description":"d","instructions":"i",
         "prepTime":1,"cookTime":2,"servings":3,
         "difficulty":"EASY","cuisineType":"ITALIAN",
         "imageUrl":"https://example.com/x.jpg","isPublic":true}
        """;

    mockMvc.perform(put("/api/recipes/" + recipe1.getId())
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Updated"));
  }

  @Test
  void updateRecipe_returnsNotFoundForRecipeOwnedByAnother() throws Exception {
    String body = """
        {"title":"Hijack","prepTime":1,"cookTime":1,"servings":1,
         "imageUrl":""}
        """;

    mockMvc.perform(put("/api/recipes/" + recipe1.getId())
            .with(user(otherUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  void updateRecipe_requiresAuthentication() throws Exception {
    mockMvc.perform(put("/api/recipes/" + recipe1.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"x\"}"))
        .andExpect(status().isUnauthorized());
  }

  // ---- DELETE /api/recipes/{id} -------------------------------------------

  @Test
  void deleteRecipe_softDeletesOwnedRecipe() throws Exception {
    mockMvc.perform(delete("/api/recipes/" + recipe2.getId()).with(user(owner)))
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    // @SQLRestriction filters deleted=true, so a normal lookup must miss.
    assertTrue(recipeRepository.findById(recipe2.getId()).isEmpty());

    // ...but the row must still exist with deleted=true (soft-delete, not
    // hard-delete). Count via native query so @SQLRestriction can't hide it.
    Number rowCount = (Number) entityManager
        .createNativeQuery("SELECT COUNT(*) FROM recipes WHERE id = :id AND deleted = TRUE")
        .setParameter("id", recipe2.getId())
        .getSingleResult();
    assertEquals(1, rowCount.intValue());
  }

  @Test
  void deleteRecipe_returnsNotFoundForRecipeOwnedByAnother() throws Exception {
    mockMvc.perform(delete("/api/recipes/" + recipe1.getId()).with(user(otherUser)))
        .andExpect(status().isNotFound());

    // Recipe must remain undeleted.
    Recipe still = recipeRepository.findById(recipe1.getId()).orElseThrow();
    assertFalse(still.getDeleted());
    assertEquals("Spaghetti Carbonara", still.getTitle());
  }

  @Test
  void deleteRecipe_requiresAuthentication() throws Exception {
    mockMvc.perform(delete("/api/recipes/" + recipe1.getId()))
        .andExpect(status().isUnauthorized());
  }
}
