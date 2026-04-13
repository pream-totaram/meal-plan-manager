package com.acedigital.meal_plan_manager.security;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.acedigital.meal_plan_manager.user.DuplicateUserException;
import com.acedigital.meal_plan_manager.user.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final UserService userService;

  public AuthController(
      AuthenticationManager authenticationManager,
      JwtService jwtService,
      UserService userService) {
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.userService = userService;
  }

  @PostMapping("/register")
  public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
    try {
      userService.createUser(request);
    } catch (DuplicateUserException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
    }
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    } catch (AuthenticationException e) {
      // Do NOT distinguish between "user not found" and "wrong password" —
      // leaking which one lets attackers enumerate usernames.
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    if (!(authentication.getPrincipal() instanceof UserDetails principal)) {
      throw new BadCredentialsException("Invalid credentials");
    }

    String token = jwtService.generateToken(principal);
    return ResponseEntity.ok(new AuthResponse(token));
  }
}
