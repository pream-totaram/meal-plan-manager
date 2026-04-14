package com.acedigital.meal_plan_manager.recipe;

import java.time.LocalDateTime;

import com.acedigital.meal_plan_manager.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saved_recipes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_saved_recipes_user_recipe", columnNames = { "user_id", "recipe_id" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedRecipe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id", nullable = false)
  @JsonIgnore
  private Recipe recipe;

  @Column(name = "saved_at", nullable = false)
  private LocalDateTime savedAt;

  @JsonProperty("userId")
  public Long getUserId() {
    return user == null ? null : user.getId();
  }

  @JsonProperty("recipeId")
  public Long getRecipeId() {
    return recipe == null ? null : recipe.getId();
  }
}
