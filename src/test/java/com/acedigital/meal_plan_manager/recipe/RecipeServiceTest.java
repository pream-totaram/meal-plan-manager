package com.acedigital.meal_plan_manager.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link RecipeService#saveRecipe} — the helper added alongside
 * the POST /api/recipes/save endpoint. We cover the three branches callers
 * care about: public recipe happy path, owner-of-private-recipe happy path,
 * and the "private recipe belonging to someone else" case that must look
 * indistinguishable from "not found" to the caller.
 */
@ExtendWith(MockitoExtension.class)
public class RecipeServiceTest {

  @Mock
  private RecipeRepository recipeRepository;

  @Mock
  private SavedRecipeRepository savedRecipeRepository;

  @Mock
  private SharedRecipeRepository sharedRecipeRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private RecipeService recipeService;

  private User caller;
  private User otherUser;

  @BeforeEach
  void setUp() {
    caller = new User();
    caller.setId(1L);
    caller.setUsername("caller");
    caller.setEmail("caller@example.com");

    otherUser = new User();
    otherUser.setId(2L);
    otherUser.setUsername("other");
    otherUser.setEmail("other@example.com");
  }

  @Test
  void saveRecipe_savesPublicRecipeOwnedBySomeoneElse() {
    Recipe recipe = Recipe.builder().id(10L).title("Pho").user(otherUser).isPublic(true).build();
    when(recipeRepository.findById(10L)).thenReturn(Optional.of(recipe));
    when(savedRecipeRepository.existsByUser_IdAndRecipe_Id(1L, 10L)).thenReturn(false);
    when(savedRecipeRepository.save(any(SavedRecipe.class)))
        .thenAnswer(invocation -> {
          SavedRecipe arg = invocation.getArgument(0);
          arg.setId(99L);
          return arg;
        });

    SavedRecipe result = recipeService.saveRecipe(10L, caller);

    assertNotNull(result);
    assertEquals(99L, result.getId());
    assertEquals(1L, result.getUserId());
    assertEquals(10L, result.getRecipeId());
    verify(savedRecipeRepository).save(any(SavedRecipe.class));
  }

  @Test
  void saveRecipe_savesPrivateRecipeOwnedByCaller() {
    Recipe recipe = Recipe.builder().id(11L).title("Draft").user(caller).isPublic(false).build();
    when(recipeRepository.findById(11L)).thenReturn(Optional.of(recipe));
    when(savedRecipeRepository.existsByUser_IdAndRecipe_Id(1L, 11L)).thenReturn(false);
    when(savedRecipeRepository.save(any(SavedRecipe.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    SavedRecipe result = recipeService.saveRecipe(11L, caller);

    assertNotNull(result);
    verify(savedRecipeRepository).save(any(SavedRecipe.class));
  }

  @Test
  void saveRecipe_throwsWhenRecipeDoesNotExist() {
    when(recipeRepository.findById(404L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> recipeService.saveRecipe(404L, caller));

    verify(savedRecipeRepository, never()).save(any());
  }

  @Test
  void saveRecipe_throwsNotFoundForPrivateRecipeOwnedByAnotherUser() {
    Recipe recipe = Recipe.builder().id(12L).title("Secret").user(otherUser).isPublic(false).build();
    when(recipeRepository.findById(12L)).thenReturn(Optional.of(recipe));

    // Identical error to the genuinely-missing case: do not leak existence.
    assertThrows(EntityNotFoundException.class,
        () -> recipeService.saveRecipe(12L, caller));

    verify(savedRecipeRepository, never()).save(any());
  }

  // ---- shareRecipe ---------------------------------------------------------

  @Test
  void shareRecipe_sharesOwnedRecipeWithTargetUser() {
    Recipe recipe = Recipe.builder().id(20L).title("Mole").user(caller).build();
    when(recipeRepository.findById(20L)).thenReturn(Optional.of(recipe));
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
    when(sharedRecipeRepository.existsByRecipe_IdAndSharedWith_Id(20L, 2L)).thenReturn(false);
    when(sharedRecipeRepository.save(any(SharedRecipe.class)))
        .thenAnswer(inv -> {
          SharedRecipe arg = inv.getArgument(0);
          arg.setId(77L);
          return arg;
        });

    SharedRecipe result = recipeService.shareRecipe(20L, "other", caller);

    assertNotNull(result);
    assertEquals(77L, result.getId());
    assertEquals(20L, result.getRecipeId());
    assertEquals(1L, result.getSharedByUserId());
    assertEquals(2L, result.getSharedWithUserId());
    verify(sharedRecipeRepository).save(any(SharedRecipe.class));
  }

  @Test
  void shareRecipe_throwsWhenRecipeMissing() {
    when(recipeRepository.findById(404L)).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> recipeService.shareRecipe(404L, "other", caller));

    verify(sharedRecipeRepository, never()).save(any());
  }

  @Test
  void shareRecipe_throwsNotFoundWhenCallerDoesNotOwnRecipe() {
    Recipe recipe = Recipe.builder().id(21L).title("Theirs").user(otherUser).build();
    when(recipeRepository.findById(21L)).thenReturn(Optional.of(recipe));

    // Same shape as missing-recipe — never reveals ownership.
    assertThrows(EntityNotFoundException.class,
        () -> recipeService.shareRecipe(21L, "other", caller));

    verify(userRepository, never()).findByUsername(any());
    verify(sharedRecipeRepository, never()).save(any());
  }

  @Test
  void shareRecipe_throwsWhenTargetUserMissing() {
    Recipe recipe = Recipe.builder().id(22L).title("Mine").user(caller).build();
    when(recipeRepository.findById(22L)).thenReturn(Optional.of(recipe));
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class,
        () -> recipeService.shareRecipe(22L, "ghost", caller));

    verify(sharedRecipeRepository, never()).save(any());
  }

  @Test
  void shareRecipe_rejectsSharingWithSelf() {
    Recipe recipe = Recipe.builder().id(23L).title("Mine").user(caller).build();
    when(recipeRepository.findById(23L)).thenReturn(Optional.of(recipe));
    when(userRepository.findByUsername("caller")).thenReturn(Optional.of(caller));

    assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> recipeService.shareRecipe(23L, "caller", caller));

    verify(sharedRecipeRepository, never()).save(any());
  }

  @Test
  void shareRecipe_isIdempotentWhenAlreadyShared() {
    Recipe recipe = Recipe.builder().id(24L).title("Mine").user(caller).build();
    when(recipeRepository.findById(24L)).thenReturn(Optional.of(recipe));
    when(userRepository.findByUsername("other")).thenReturn(Optional.of(otherUser));
    when(sharedRecipeRepository.existsByRecipe_IdAndSharedWith_Id(24L, 2L)).thenReturn(true);

    SharedRecipe result = recipeService.shareRecipe(24L, "other", caller);

    assertNotNull(result);
    assertEquals(24L, result.getRecipeId());
    assertEquals(2L, result.getSharedWithUserId());
    verify(sharedRecipeRepository, never()).save(any());
  }

  @Test
  void saveRecipe_throwsForOrphanRecipeWhenNotPublic() {
    // recipe.getUser() == null exercises the short-circuit branch in the
    // ownership check. Such rows shouldn't exist in production (FK is
    // NOT NULL) but defending against the null guarantees 100% branch
    // coverage of the && expression.
    Recipe recipe = Recipe.builder().id(50L).title("Orphan").user(null).isPublic(false).build();
    when(recipeRepository.findById(50L)).thenReturn(Optional.of(recipe));

    assertThrows(EntityNotFoundException.class,
        () -> recipeService.saveRecipe(50L, caller));
  }

  @Test
  void shareRecipe_throwsNotFoundForOrphanRecipe() {
    Recipe recipe = Recipe.builder().id(51L).title("Orphan").user(null).build();
    when(recipeRepository.findById(51L)).thenReturn(Optional.of(recipe));

    assertThrows(EntityNotFoundException.class,
        () -> recipeService.shareRecipe(51L, "other", caller));
  }

  @Test
  void saveRecipe_isIdempotentWhenAlreadySaved() {
    Recipe recipe = Recipe.builder().id(13L).title("Ramen").user(otherUser).isPublic(true).build();
    when(recipeRepository.findById(13L)).thenReturn(Optional.of(recipe));
    when(savedRecipeRepository.existsByUser_IdAndRecipe_Id(1L, 13L)).thenReturn(true);

    SavedRecipe result = recipeService.saveRecipe(13L, caller);

    assertNotNull(result);
    assertEquals(1L, result.getUserId());
    assertEquals(13L, result.getRecipeId());
    verify(savedRecipeRepository, never()).save(any());
  }
}
