package com.acedigital.meal_plan_manager.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.acedigital.meal_plan_manager.recipe.RecipeRepository;
import com.acedigital.meal_plan_manager.recipe.RecipeService;

import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration
public class WebSecurityTestConfig {
  @Bean
  public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf((csrf) -> csrf.disable())
        .authorizeHttpRequests((requests) -> requests
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated());
    // .addFilterBefore(filter, beforeFilter);
    return http.build();
  }

  @Bean
  @Primary
  public UserDetailsService userDetailsService(PasswordEncoder encoder) {
    return username -> User.withUsername("username")
        .password(encoder.encode("password"))
        .authorities("AUTH")
        .build();
  }

  @Bean
  public RecipeService recipeService(RecipeRepository recipeRepository) {
    return new RecipeService(recipeRepository);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
