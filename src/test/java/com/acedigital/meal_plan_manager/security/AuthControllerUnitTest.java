package com.acedigital.meal_plan_manager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.acedigital.meal_plan_manager.user.DuplicateUserException;
import com.acedigital.meal_plan_manager.user.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit-level tests for the corners of {@link AuthController} that the
 * functional test cannot reach — specifically the
 * "principal is not a {@code UserDetails}" guard in
 * {@link AuthController#login}, which is impossible to trigger through a
 * real DaoAuthenticationProvider.
 */
public class AuthControllerUnitTest {

  @Test
  void login_throwsBadCredentialsWhenPrincipalIsNotUserDetails() {
    AuthenticationManager am = mock(AuthenticationManager.class);
    JwtService jwt = mock(JwtService.class);
    UserService users = mock(UserService.class);

    // Principal is a plain String — exercises the !instanceof branch.
    Authentication auth = new UsernamePasswordAuthenticationToken("not-userdetails", null);
    when(am.authenticate(any())).thenReturn(auth);

    AuthController controller = new AuthController(am, jwt, users);

    assertThrows(BadCredentialsException.class,
        () -> controller.login(new LoginRequest("u", "passwordlongenough")));
  }

  @Test
  void register_translatesDuplicateUserExceptionToConflict() {
    AuthenticationManager am = mock(AuthenticationManager.class);
    JwtService jwt = mock(JwtService.class);
    UserService users = mock(UserService.class);
    when(users.createUser(any())).thenThrow(new DuplicateUserException("dup"));

    AuthController controller = new AuthController(am, jwt, users);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> controller.register(new RegisterRequest(
            "user", "u@example.com", "correcthorsebattery")));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
  }
}
