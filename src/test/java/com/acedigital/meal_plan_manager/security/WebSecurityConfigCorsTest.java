package com.acedigital.meal_plan_manager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Targets the non-empty branch of
 * {@link WebSecurityConfig#corsConfigurationSource()}. The default test
 * profile leaves {@code application.security.cors.allowed-origins} unset,
 * so the production code's "explicit origins provided" path is otherwise
 * never executed.
 */
public class WebSecurityConfigCorsTest {

  @Test
  void corsConfigurationSource_allowsZeroOriginsWhenCsvNull() {
    // Default branch — production wiring leaves origins empty (no
    // cross-origin calls allowed).
    WebSecurityConfig cfg = new WebSecurityConfig();
    ReflectionTestUtils.setField(cfg, "allowedOriginsCsv", null);

    UrlBasedCorsConfigurationSource src =
        (UrlBasedCorsConfigurationSource) cfg.corsConfigurationSource();
    CorsConfiguration applied = src.getCorsConfigurations().get("/**");

    assertEquals(0, applied.getAllowedOrigins().size());
  }

  @Test
  void corsConfigurationSource_appliesExplicitAllowedOrigins() {
    WebSecurityConfig cfg = new WebSecurityConfig();
    ReflectionTestUtils.setField(cfg, "allowedOriginsCsv",
        "https://app.example.com, https://admin.example.com");

    UrlBasedCorsConfigurationSource src =
        (UrlBasedCorsConfigurationSource) cfg.corsConfigurationSource();
    CorsConfiguration applied = src.getCorsConfigurations().get("/**");

    assertEquals(2, applied.getAllowedOrigins().size());
    assertTrue(applied.getAllowedOrigins().contains("https://app.example.com"));
    assertTrue(applied.getAllowedOrigins().contains("https://admin.example.com"));
    // allowCredentials must remain false even with an explicit allow-list.
    assertEquals(false, applied.getAllowCredentials());
  }
}
