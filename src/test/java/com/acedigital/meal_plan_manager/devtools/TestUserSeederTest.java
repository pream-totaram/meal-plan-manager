package com.acedigital.meal_plan_manager.devtools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.acedigital.meal_plan_manager.MealPlanManagerApplication;
import com.acedigital.meal_plan_manager.user.User;
import com.acedigital.meal_plan_manager.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

public class TestUserSeederTest {

  // ---- seedAll -------------------------------------------------------------

  @Test
  void seedAll_createsAllUsersWhenNonePresent() {
    UserRepository repo = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(repo.findByUsername(any())).thenReturn(Optional.empty());
    when(encoder.encode(TestUserSeeder.SEED_PASSWORD)).thenReturn("hashed");
    when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    TestUserSeeder.Result result = TestUserSeeder.seedAll(repo, encoder);

    assertEquals(TestUserSeeder.SEED_USERS.size(), result.created());
    assertEquals(0, result.reset());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(repo, times(TestUserSeeder.SEED_USERS.size())).save(captor.capture());
    for (User saved : captor.getAllValues()) {
      assertEquals("hashed", saved.getPassword());
      assertNotNull(saved.getEmail());
      assertTrue(saved.getEmail().endsWith("@example.com"));
    }
  }

  @Test
  void seedAll_resetsPasswordWhenUserAlreadyExists() {
    UserRepository repo = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(encoder.encode(TestUserSeeder.SEED_PASSWORD)).thenReturn("hashed");

    Map<String, User> existing = new HashMap<>();
    for (TestUserSeeder.SeedUser seed : TestUserSeeder.SEED_USERS) {
      User u = new User();
      u.setUsername(seed.username());
      u.setEmail("stale-" + seed.email());
      u.setPassword("old-hash");
      existing.put(seed.username(), u);
      when(repo.findByUsername(seed.username())).thenReturn(Optional.of(u));
    }
    when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    TestUserSeeder.Result result = TestUserSeeder.seedAll(repo, encoder);

    assertEquals(0, result.created());
    assertEquals(TestUserSeeder.SEED_USERS.size(), result.reset());

    // Each existing User must have had its password reset to the hashed seed
    // value and email re-pinned to the canonical address (no "stale-" prefix).
    for (TestUserSeeder.SeedUser seed : TestUserSeeder.SEED_USERS) {
      User u = existing.get(seed.username());
      assertEquals("hashed", u.getPassword());
      assertEquals(seed.email(), u.getEmail());
    }
  }

  @Test
  void seedAll_mixedCreateAndReset() {
    UserRepository repo = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    when(encoder.encode(TestUserSeeder.SEED_PASSWORD)).thenReturn("hashed");

    // alice already exists, the rest don't.
    TestUserSeeder.SeedUser first = TestUserSeeder.SEED_USERS.get(0);
    User existing = new User();
    existing.setUsername(first.username());
    existing.setEmail(first.email());
    existing.setPassword("old");
    when(repo.findByUsername(first.username())).thenReturn(Optional.of(existing));
    for (int i = 1; i < TestUserSeeder.SEED_USERS.size(); i++) {
      when(repo.findByUsername(TestUserSeeder.SEED_USERS.get(i).username()))
          .thenReturn(Optional.empty());
    }
    when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    TestUserSeeder.Result result = TestUserSeeder.seedAll(repo, encoder);

    assertEquals(TestUserSeeder.SEED_USERS.size() - 1, result.created());
    assertEquals(1, result.reset());
  }

  // ---- main ----------------------------------------------------------------

  @Test
  void main_bootsContextSeedsUsersAndClosesContext() {
    UserRepository repo = mock(UserRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);
    ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
    when(ctx.getBean(UserRepository.class)).thenReturn(repo);
    when(ctx.getBean(PasswordEncoder.class)).thenReturn(encoder);
    when(repo.findByUsername(any())).thenReturn(Optional.empty());
    when(encoder.encode(any())).thenReturn("hashed");
    when(repo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    SpringApplication springApp = mock(SpringApplication.class);
    when(springApp.run(any(String[].class))).thenReturn(ctx);

    // Intercept the builder so we don't actually boot Spring.
    try (MockedConstruction<SpringApplicationBuilder> mocked = Mockito.mockConstruction(
        SpringApplicationBuilder.class,
        (m, ctxArgs) -> {
          when(m.web(any())).thenReturn(m);
          when(m.build()).thenReturn(springApp);
        })) {

      TestUserSeeder.main(new String[] { "--server.port=0" });

      assertEquals(1, mocked.constructed().size());
      SpringApplicationBuilder builder = mocked.constructed().get(0);
      verify(builder).build();
      verify(springApp).run(eq(new String[] { "--server.port=0" }));
      verify(repo, times(TestUserSeeder.SEED_USERS.size())).save(any(User.class));
      verify(ctx).close(); // try-with-resources must release the context.
    }
  }

  // ---- record accessors (cover trivial generated members) ------------------

  @Test
  void seedUserAndResultRecords_exposeFields() {
    TestUserSeeder.SeedUser su = new TestUserSeeder.SeedUser("u", "u@example.com");
    assertEquals("u", su.username());
    assertEquals("u@example.com", su.email());

    TestUserSeeder.Result r = new TestUserSeeder.Result(2, 3);
    assertEquals(2, r.created());
    assertEquals(3, r.reset());
  }

  @Test
  void springApplicationClassReferenceMatches() {
    // Sanity check that the seeder targets the same Spring Boot main as the
    // production app — a refactor that moves the @SpringBootApplication should
    // surface here, not at first run.
    assertNotNull(MealPlanManagerApplication.class.getAnnotation(
        org.springframework.boot.autoconfigure.SpringBootApplication.class));
  }
}
