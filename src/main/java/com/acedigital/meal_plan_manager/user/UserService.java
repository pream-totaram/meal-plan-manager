package com.acedigital.meal_plan_manager.user;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.acedigital.meal_plan_manager.security.RegisterRequest;

public class UserService {

  private UserRepository repository;
  private PasswordEncoder encoder;

  public UserService(UserRepository repository, PasswordEncoder encoder) {
    this.repository = repository;
    this.encoder = encoder;

  }

  public void createUser(RegisterRequest request) {
    String encodePassword = encoder.encode(request.password());
    User user = new User();
    user.setUsername(request.username());
    user.setPassword(encodePassword);
    user.setEmail(request.email());

    repository.save(user);
  }

}
