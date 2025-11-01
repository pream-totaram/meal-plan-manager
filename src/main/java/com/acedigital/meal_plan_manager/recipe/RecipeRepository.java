package com.acedigital.meal_plan_manager.recipe;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

  List<Recipe> findAll();

  @Modifying
  @Transactional
  @Query("UPDATE Recipe r SET r.deleted = true, r.deletedAt = CURRENT_TIMESTAMP WHERE r.id = :id")
  void softDelete(@Param("id") Long id);
}
