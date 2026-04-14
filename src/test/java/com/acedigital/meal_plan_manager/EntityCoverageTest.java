package com.acedigital.meal_plan_manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.acedigital.meal_plan_manager.recipe.Recipe;
import com.acedigital.meal_plan_manager.recipe.SavedRecipe;
import com.acedigital.meal_plan_manager.recipe.SharedRecipe;
import com.acedigital.meal_plan_manager.user.User;

import org.junit.jupiter.api.Test;

/**
 * Pinch-hit coverage for entity helpers whose null branches and
 * lesser-used constructors aren't exercised by the controller / repository
 * tests. These exist solely to satisfy the 100% branch gate — none of
 * them are interesting individually.
 */
public class EntityCoverageTest {

  @Test
  void user_twoArgConstructor_setsUsernameAndEmail() {
    User u = new User("alice", "a@example.com");
    assertEquals("alice", u.getUsername());
    assertEquals("a@example.com", u.getEmail());
    assertNull(u.getId());
  }

  // ---- SavedRecipe.getUserId / getRecipeId null branches -------------------

  @Test
  void savedRecipe_jsonAccessors_nullWhenAssociationsAbsent() {
    SavedRecipe sr = new SavedRecipe();
    assertNull(sr.getUserId());
    assertNull(sr.getRecipeId());
  }

  @Test
  void savedRecipe_jsonAccessors_returnIdsWhenAssociationsPresent() {
    User u = new User();
    u.setId(7L);
    Recipe r = Recipe.builder().id(8L).build();
    SavedRecipe sr = SavedRecipe.builder().user(u).recipe(r).build();
    assertEquals(7L, sr.getUserId());
    assertEquals(8L, sr.getRecipeId());
  }

  // ---- SharedRecipe accessors null branches -------------------------------

  @Test
  void sharedRecipe_jsonAccessors_nullWhenAssociationsAbsent() {
    SharedRecipe sr = new SharedRecipe();
    assertNull(sr.getRecipeId());
    assertNull(sr.getSharedByUserId());
    assertNull(sr.getSharedWithUserId());
  }

  @Test
  void sharedRecipe_jsonAccessors_returnIdsWhenAssociationsPresent() {
    User by = new User();
    by.setId(1L);
    User with = new User();
    with.setId(2L);
    Recipe r = Recipe.builder().id(3L).build();
    SharedRecipe sr = SharedRecipe.builder().recipe(r).sharedBy(by).sharedWith(with).build();
    assertEquals(3L, sr.getRecipeId());
    assertEquals(1L, sr.getSharedByUserId());
    assertEquals(2L, sr.getSharedWithUserId());
  }

  // ---- Recipe.getUserId null branch ---------------------------------------

  @Test
  void recipe_getUserId_nullWhenUserAbsent() {
    Recipe r = new Recipe();
    assertNull(r.getUserId());
  }
}
