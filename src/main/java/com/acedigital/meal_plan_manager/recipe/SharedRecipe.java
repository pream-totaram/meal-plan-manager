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
@Table(name = "shared_recipes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_shared_recipes_recipe_target",
        columnNames = { "recipe_id", "shared_with_user_id" })
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedRecipe {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipe_id", nullable = false)
  @JsonIgnore
  private Recipe recipe;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shared_by_user_id", nullable = false)
  @JsonIgnore
  private User sharedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shared_with_user_id", nullable = false)
  @JsonIgnore
  private User sharedWith;

  @Column(name = "shared_at", nullable = false)
  private LocalDateTime sharedAt;

  @JsonProperty("recipeId")
  public Long getRecipeId() {
    return recipe == null ? null : recipe.getId();
  }

  @JsonProperty("sharedByUserId")
  public Long getSharedByUserId() {
    return sharedBy == null ? null : sharedBy.getId();
  }

  @JsonProperty("sharedWithUserId")
  public Long getSharedWithUserId() {
    return sharedWith == null ? null : sharedWith.getId();
  }
}
