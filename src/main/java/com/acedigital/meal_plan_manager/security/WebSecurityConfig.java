package com.acedigital.meal_plan_manager.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.acedigital.meal_plan_manager.recipe.RecipeRepository;
import com.acedigital.meal_plan_manager.recipe.RecipeService;
import com.acedigital.meal_plan_manager.user.JpaUserDetailsService;
import com.acedigital.meal_plan_manager.user.UserRepository;
import com.acedigital.meal_plan_manager.user.UserService;

@Configuration
public class WebSecurityConfig {
  @Value("${application.security.jwt.secret-key}")
  private String jwtSecret;

  @Value("${application.security.jwt.expiration}")
  private long jwtExpiration;

  JwtService jwtService = new JwtService(jwtSecret, jwtExpiration);

  UserDetailsService userDetailsService = new JpaUserDetailsService();

  PasswordEncoder encoder = new BCryptPasswordEncoder();

  @Bean
  public RecipeService recipeService(RecipeRepository recipeRepository) {
    return new RecipeService(recipeRepository);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return encoder;
  }

  @Bean
  public JwtService jwtService() {
    return this.jwtService;
  }

  @Bean
  UserDetailsService userDetailsService() {
    return userDetailsService;
  }

  @Bean
  AuthenticationManager authenticationManager() {
    return new AuthenticationManager();
  }

  @Bean
  UserService userService(UserRepository userRepository) {
    return new UserService(userRepository, encoder);
  }

  @Bean
  JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter(this.jwtService, userDetailsService);
  }
}
