package com.acedigital.meal_plan_manager.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.acedigital.meal_plan_manager.security.RegisterRequest;

@Service
public class UserService {

  private final UserRepository repository;
  private final PasswordEncoder encoder;

  public UserService(UserRepository repository, PasswordEncoder encoder) {
    this.repository = repository;
    this.encoder = encoder;
  }

  public User createUser(RegisterRequest request) {
    // Defense in depth: even if validation annotations are bypassed by
    // a direct caller, we refuse to create a second user with the same
    // username. DB-level unique constraint is the authoritative check.
    if (repository.findByUsername(request.username()).isPresent()) {
      throw new DuplicateUserException("Username is already taken");
    }

    User user = new User();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setPassword(encoder.encode(request.password()));
    return repository.save(user);
  }
}
