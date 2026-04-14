package com.acedigital.meal_plan_manager.recipe;

import java.time.LocalDateTime;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "recipes")
@SQLDelete(sql = "UPDATE recipes SET deleted = true, deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
public class Recipe {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @Column(nullable = false)
  private String title;

  @Column
  private String description;

  @Column
  private String instructions;

  @Column
  private Integer prepTime;

  @Column
  private Integer cookTime;

  @Column
  private Integer servings;

  @Column
  private Difficulty difficulty;

  @Column
  private CuisineType cuisineType;

  // Storing a bounded String (URL or server-generated UUID filename), never a
  // java.io.File bound from JSON. A File field would let a caller set
  // {"image":"/etc/passwd"} and leak server paths via getAbsolutePath().
  @Column(name = "image_url", length = 512)
  private String imageUrl;

  @Column
  private Boolean isPublic;

  @Column(name = "average_rating", columnDefinition = "numeric")
  private Double averageRating;

  @Column
  private Integer totalReviews;

  @Column
  @JsonIgnore
  @Builder.Default
  private Boolean deleted = false;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @JsonProperty("userId")
  public Long getUserId() {
    return user == null ? null : user.getId();
  }
}
