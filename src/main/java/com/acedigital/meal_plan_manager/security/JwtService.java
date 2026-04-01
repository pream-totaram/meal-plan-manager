package com.acedigital.meal_plan_manager.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;

@Component
public class JwtService {

  private Key key = Keys.hmacShaKeyFor(System.getenv("JWT_KEY").getBytes());

  public String extractUsername(String token) {
    return Jwts.parser().build().parseEncryptedContent(token).toString();

  }

}
