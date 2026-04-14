package com.acedigital.meal_plan_manager.recipe;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

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

/**
 * End-to-end test for POST /api/recipes/save. Uses MockMvc + a real H2-backed
 * Spring context (the test profile substitutes Liquibase for JPA's
 * {@code create-drop}, so {@code saved_recipes} is materialised from the
 * entity). The {@code user(...)} request post-processor injects an
 * authenticated principal — the controller resolves it via
 * {@code @AuthenticationPrincipal}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SaveRecipeFunctionalTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RecipeRepository recipeRepository;

  @Autowired
  private SavedRecipeRepository savedRecipeRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private ObjectMapper objectMapper;

  private User caller;
  private User author;
  private Recipe publicRecipe;
  private Recipe privateRecipeOwnedByCaller;
  private Recipe privateRecipeOwnedByAuthor;

  @BeforeEach
  void setUp() {
    caller = new User();
    caller.setUsername("caller-" + System.nanoTime());
    caller.setEmail(caller.getUsername() + "@example.com");
    caller.setPassword(passwordEncoder.encode("password"));
    caller = userRepository.save(caller);

    author = new User();
    author.setUsername("author-" + System.nanoTime());
    author.setEmail(author.getUsername() + "@example.com");
    author.setPassword(passwordEncoder.encode("password"));
    author = userRepository.save(author);

    publicRecipe = recipeRepository.save(Recipe.builder()
        .title("Public Pho").user(author).isPublic(true).build());
    privateRecipeOwnedByCaller = recipeRepository.save(Recipe.builder()
        .title("My Draft").user(caller).isPublic(false).build());
    privateRecipeOwnedByAuthor = recipeRepository.save(Recipe.builder()
        .title("Author's Secret").user(author).isPublic(false).build());
  }

  @Test
  void savesPublicRecipe() throws Exception {
    String body = objectMapper.writeValueAsString(
        new java.util.HashMap<String, Object>() {{ put("recipeId", publicRecipe.getId()); }});

    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("/api/recipes/" + publicRecipe.getId())));

    assertTrue(savedRecipeRepository
        .existsByUser_IdAndRecipe_Id(caller.getId(), publicRecipe.getId()));
  }

  @Test
  void savesPrivateRecipeOwnedByCaller() throws Exception {
    String body = "{\"recipeId\":" + privateRecipeOwnedByCaller.getId() + "}";

    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    assertTrue(savedRecipeRepository
        .existsByUser_IdAndRecipe_Id(caller.getId(), privateRecipeOwnedByCaller.getId()));
  }

  @Test
  void rejectsPrivateRecipeBelongingToAnotherUser() throws Exception {
    String body = "{\"recipeId\":" + privateRecipeOwnedByAuthor.getId() + "}";

    // Same response shape as a missing recipe — must not leak existence.
    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound());

    assertEquals(0L, savedRecipeRepository.count());
  }

  @Test
  void returnsNotFoundForMissingRecipe() throws Exception {
    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipeId\":999999}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsMissingRecipeIdAsBadRequest() throws Exception {
    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void requiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/recipes/save")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipeId\":" + publicRecipe.getId() + "}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void duplicateSaveIsIdempotent() throws Exception {
    String body = "{\"recipeId\":" + publicRecipe.getId() + "}";

    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/recipes/save")
            .with(user(caller))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    assertEquals(1L, savedRecipeRepository.count());
  }
}
