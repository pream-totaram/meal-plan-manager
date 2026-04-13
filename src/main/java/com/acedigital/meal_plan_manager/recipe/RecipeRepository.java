package com.acedigital.meal_plan_manager.recipe;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

  /**
   * Owner-scoped list: never return another user's recipes from a
   * non-public endpoint. Relied on by {@link RecipeService#getRecipesFor}.
   * The {@code user_id} traversal (with underscore) tells Spring Data to
   * navigate {@code Recipe.user.id}.
   */
  List<Recipe> findAllByUser_Id(Long userId);

  /**
   * Owner-scoped single-record lookup. Used for GET/PUT/DELETE to enforce
   * BOLA at the query level instead of the controller layer.
   */
  Optional<Recipe> findByIdAndUser_Id(Long id, Long userId);

  /**
   * Owner-scoped soft delete. We intentionally include {@code user_id} in
   * the WHERE clause so that even if a caller forges a recipe id they do
   * not own, no rows are affected.
   */
  @Modifying
  @Transactional
  @Query("UPDATE Recipe r SET r.deleted = true, r.deletedAt = CURRENT_TIMESTAMP "
      + "WHERE r.id = :id AND r.user.id = :userId")
  int softDeleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
