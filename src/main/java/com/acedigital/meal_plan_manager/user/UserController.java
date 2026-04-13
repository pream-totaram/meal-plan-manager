package com.acedigital.meal_plan_manager.user;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

  @GetMapping("/")
  public ResponseEntity<Map<String, String>> index() {
    return ResponseEntity.ok(Map.of("message", "Hello, World!"));
  }
}
