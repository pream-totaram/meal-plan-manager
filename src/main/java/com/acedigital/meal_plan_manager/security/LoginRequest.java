package com.acedigital.meal_plan_manager.security;

public record LoginRequest(String username, String password) {
  public LoginRequest {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("Username cannot be empty");
    }
  }
}
