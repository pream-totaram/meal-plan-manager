package com.acedigital.meal_plan_manager.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acedigital.meal_plan_manager.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end tests for {@link AuthController} — the only two endpoints
 * permitted to be reached without authentication: {@code POST
 * /api/auth/register} and {@code POST /api/auth/login}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthFunctionalTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  // ---- /api/auth/register --------------------------------------------------

  @Test
  void register_createsUserAndReturns201() throws Exception {
    String username = "user-" + System.nanoTime();
    String body = """
        {"username":"%s","email":"%s@example.com","password":"correcthorsebattery"}
        """.formatted(username, username);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    assertTrue(userRepository.findByUsername(username).isPresent());
  }

  @Test
  void register_returnsConflictForDuplicateUsername() throws Exception {
    String username = "dup-" + System.nanoTime();
    String body = """
        {"username":"%s","email":"%s@example.com","password":"correcthorsebattery"}
        """.formatted(username, username);

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    String body2 = """
        {"username":"%s","email":"%s2@example.com","password":"correcthorsebattery"}
        """.formatted(username, username);
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body2))
        .andExpect(status().isConflict());
  }

  @Test
  void register_rejectsShortPassword() throws Exception {
    // Below the 12-char floor enforced by RegisterRequest.
    String body = """
        {"username":"shorty","email":"shorty@example.com","password":"short"}
        """;
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_rejectsInvalidUsernameCharacters() throws Exception {
    String body = """
        {"username":"bad name!","email":"x@example.com","password":"correcthorsebattery"}
        """;
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_rejectsMalformedEmail() throws Exception {
    String body = """
        {"username":"goodname","email":"not-an-email","password":"correcthorsebattery"}
        """;
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
  }

  // ---- /api/auth/login -----------------------------------------------------

  @Test
  void login_returnsTokenForValidCredentials() throws Exception {
    String username = "loginok-" + System.nanoTime();
    String register = """
        {"username":"%s","email":"%s@example.com","password":"correcthorsebattery"}
        """.formatted(username, username);
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(register))
        .andExpect(status().isCreated());

    String login = """
        {"username":"%s","password":"correcthorsebattery"}
        """.formatted(username);

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(login))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void login_returns401ForWrongPassword() throws Exception {
    String username = "loginbad-" + System.nanoTime();
    String register = """
        {"username":"%s","email":"%s@example.com","password":"correcthorsebattery"}
        """.formatted(username, username);
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(register))
        .andExpect(status().isCreated());

    String login = """
        {"username":"%s","password":"wrongwrongwrong"}
        """.formatted(username);
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(login))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_returns401ForUnknownUser() throws Exception {
    // Identical response to the wrong-password case to defeat user
    // enumeration. The status check is the contract; we don't assert on
    // body shape because that is what an attacker would also see.
    String login = """
        {"username":"nobody-%d","password":"correcthorsebattery"}
        """.formatted(System.nanoTime());
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(login))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_rejectsBlankFieldsAsBadRequest() throws Exception {
    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"\",\"password\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
