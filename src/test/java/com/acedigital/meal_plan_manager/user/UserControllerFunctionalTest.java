package com.acedigital.meal_plan_manager.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coverage for the lone {@link UserController} endpoint — GET /. It is a
 * trivial "hello world" response, but security policy still requires the
 * caller to be authenticated, and that contract deserves an explicit test
 * so a future routing change cannot quietly open it up.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class UserControllerFunctionalTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  private User caller;

  @BeforeEach
  void setUp() {
    caller = new User();
    caller.setUsername("indexcaller-" + System.nanoTime());
    caller.setEmail(caller.getUsername() + "@example.com");
    caller.setPassword(passwordEncoder.encode("password"));
    caller = userRepository.save(caller);
  }

  @Test
  void index_returnsHelloWorldWhenAuthenticated() throws Exception {
    mockMvc.perform(get("/").with(user(caller)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Hello, World!"));
  }

  @Test
  void index_requiresAuthentication() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isUnauthorized());
  }
}
