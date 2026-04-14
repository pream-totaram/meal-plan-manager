package com.acedigital.meal_plan_manager.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;

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
 * End-to-end test for POST /api/recipes/share. Mirrors
 * {@link SaveRecipeFunctionalTest}: real Spring context, H2 schema from JPA
 * entities, MockMvc with an injected authenticated principal.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ShareRecipeFunctionalTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private RecipeRepository recipeRepository;

  @Autowired
  private SharedRecipeRepository sharedRecipeRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User owner;
  private User recipient;
  private User stranger;
  private Recipe ownedRecipe;
  private Recipe strangerRecipe;

  @BeforeEach
  void setUp() {
    owner = persistUser("owner");
    recipient = persistUser("recipient");
    stranger = persistUser("stranger");

    ownedRecipe = recipeRepository.save(Recipe.builder()
        .title("Owned").user(owner).isPublic(false).build());
    strangerRecipe = recipeRepository.save(Recipe.builder()
        .title("Stranger's").user(stranger).isPublic(false).build());
  }

  private User persistUser(String prefix) {
    User u = new User();
    u.setUsername(prefix + "-" + System.nanoTime());
    u.setEmail(u.getUsername() + "@example.com");
    u.setPassword(passwordEncoder.encode("password"));
    return userRepository.save(u);
  }

  private String shareBody(Long recipeId, String username) {
    return "{\"recipeId\":" + recipeId + ",\"shareWithUsername\":\"" + username + "\"}";
  }

  @Test
  void sharesOwnedRecipe() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(shareBody(ownedRecipe.getId(), recipient.getUsername())))
        .andExpect(status().isCreated());

    assertTrue(sharedRecipeRepository
        .existsByRecipe_IdAndSharedWith_Id(ownedRecipe.getId(), recipient.getId()));
  }

  @Test
  void rejectsSharingARecipeNotOwnedByCaller() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(shareBody(strangerRecipe.getId(), recipient.getUsername())))
        .andExpect(status().isNotFound());

    assertEquals(0L, sharedRecipeRepository.count());
  }

  @Test
  void returnsNotFoundForMissingRecipe() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(shareBody(999999L, recipient.getUsername())))
        .andExpect(status().isNotFound());
  }

  @Test
  void returnsNotFoundForUnknownTargetUsername() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(shareBody(ownedRecipe.getId(), "no-such-user")))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsSharingWithSelf() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(shareBody(ownedRecipe.getId(), owner.getUsername())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsMissingFieldsAsBadRequest() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void requiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/recipes/share")
            .contentType(MediaType.APPLICATION_JSON)
            .content(shareBody(ownedRecipe.getId(), recipient.getUsername())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void duplicateShareIsIdempotent() throws Exception {
    String body = shareBody(ownedRecipe.getId(), recipient.getUsername());

    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/recipes/share")
            .with(user(owner))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    assertEquals(1L, sharedRecipeRepository.count());
  }
}
