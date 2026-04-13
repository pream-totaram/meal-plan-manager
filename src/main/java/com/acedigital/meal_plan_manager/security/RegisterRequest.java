package com.acedigital.meal_plan_manager.security;

public record RegisterRequest(String username, String email, String password) {
}
