package com.acedigital.meal_plan_manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the full Spring context (including Spring Security,
 * JPA, Liquibase disabled on test profile, and all controllers) must
 * boot cleanly. This catches misconfigurations like missing
 * SecurityFilterChain beans or unresolvable {@code @Value} placeholders.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

  @Test
  void contextLoads() {
    // Intentionally empty — test passes if the context loads.
  }
}
