package com.acedigital.meal_plan_manager.user;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {


  @GetMapping("/")
  public ResponseEntity<String> index() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    return ResponseEntity.ok()
    .headers(headers)
    .body("{\"Hello, World!\"}");
  }
}
