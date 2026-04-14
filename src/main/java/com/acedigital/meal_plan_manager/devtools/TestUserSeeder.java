package com.acedigital.meal_plan_manager.devtools;

import java.util.List;

import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.acedigital.meal_plan_manager.MealPlanManagerApplication;

/**
 * Standalone tool for materializing a known set of test accounts in the
 * configured database. Intentionally NOT a {@code CommandLineRunner} — we
 * never want this to fire automatically as a side effect of starting the
 * web app. Run explicitly via Maven:
 *
 * <pre>
 * ./mvnw exec:java -Dexec.mainClass=com.acedigital.meal_plan_manager.devtools.TestUserSeeder
 * </pre>
 *
 * Each call boots a non-web Spring context against whatever datasource
 * the active profile points at, upserts the {@link #SEED_USERS} list
 * (existing accounts have their password reset to the seed value), and
 * exits.
 */
public final class TestUserSeeder {

  private static final Logger log = LoggerFactory.getLogger(TestUserSeeder.class);

  /** Shared password for every seeded account — long enough for the bcrypt floor. */
  static final String SEED_PASSWORD = "correcthorsebattery";

  /** Fixed list of accounts the seeder maintains. Order is irrelevant. */
  static final List<SeedUser> SEED_USERS = List.of(
      new SeedUser("alice", "alice@example.com"),
      new SeedUser("bob", "bob@example.com"),
      new SeedUser("carol", "carol@example.com"),
      new SeedUser("dave", "dave@example.com"),
      new SeedUser("eve", "eve@example.com"));

  private TestUserSeeder() {
    // utility class
  }

  public static void main(String[] args) {
    SpringApplication app = new SpringApplicationBuilder(MealPlanManagerApplication.class)
        .web(WebApplicationType.NONE)
        .build();
    try (ConfigurableApplicationContext ctx = app.run(args)) {
      UserRepository users = ctx.getBean(UserRepository.class);
      PasswordEncoder encoder = ctx.getBean(PasswordEncoder.class);
      Result result = seedAll(users, encoder);
      log.info("Seed complete: {} created, {} reset.", result.created(), result.reset());
    }
  }

  /**
   * Upserts {@link #SEED_USERS}. Returns counts of newly-created vs
   * password-reset accounts so callers (and tests) can assert the
   * outcome without scraping logs.
   */
  static Result seedAll(UserRepository users, PasswordEncoder encoder) {
    int created = 0;
    int reset = 0;
    String hashed = encoder.encode(SEED_PASSWORD);
    for (SeedUser seed : SEED_USERS) {
      User existing = users.findByUsername(seed.username()).orElse(null);
      if (existing != null) {
        existing.setPassword(hashed);
        existing.setEmail(seed.email());
        users.save(existing);
        reset++;
      } else {
        User u = new User();
        u.setUsername(seed.username());
        u.setEmail(seed.email());
        u.setPassword(hashed);
        users.save(u);
        created++;
      }
    }
    return new Result(created, reset);
  }

  record SeedUser(String username, String email) {
  }

  record Result(int created, int reset) {
  }
}
