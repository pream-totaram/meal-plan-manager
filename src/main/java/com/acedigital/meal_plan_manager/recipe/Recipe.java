package com.acedigital.meal_plan_manager.recipe;

import java.io.File;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.acedigital.meal_plan_manager.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recipes")
@SQLDelete(sql = "UPDATE recipes SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recipe {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @Column(nullable = false)
  String title;

  @Column
  String description;

  @Column
  String instructions;

  @Column
  Integer prepTime;

  @Column
  Integer cookTime;

  @Column
  Integer servings;

  @Column
  Difficulty difficulty;

  @Column
  CuisineType cuisineType;

  @Column(name = "image_url")
  @JsonIgnore
  File image;

  @Column
  Boolean isPublic;

  @Column
  Double averageRating;

  @Column
  Integer totalReviews;

  @Column
  @JsonIgnore
  @Builder.Default
  Boolean deleted = false;

  @Column(name = "deleted_at")
  Timestamp deletedAt;

  @JsonProperty("imagePath")
  public String getImagePath() {
    return image == null ? "" : image.getAbsolutePath();
  }

  @JsonProperty("userId")
  public Long getUserId() {
    return user == null ? null : user.getId();
  }

  public void applyUpdate(Recipe update) {
    this.title = update.getTitle();
    this.description = update.getDescription();
    this.instructions = update.getInstructions();
    this.prepTime = update.getPrepTime();
    this.cookTime = update.getCookTime();
    this.servings = update.getServings();
    this.difficulty = update.getDifficulty();
    this.cuisineType = update.getCuisineType();
    this.image = update.getImage();
    this.isPublic = update.getIsPublic();
    this.averageRating = update.getAverageRating();
    this.totalReviews = update.getTotalReviews();
  }
}
