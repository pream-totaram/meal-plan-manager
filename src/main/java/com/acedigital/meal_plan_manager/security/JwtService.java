package com.acedigital.meal_plan_manager.security;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

@Component
public class JwtService {

  private String jwtSecret;
  private long jwtExpiration;

  public JwtService(String jwtSecret, long jwtExpiration) {
    this.jwtSecret = jwtSecret;
    this.jwtExpiration = jwtExpiration;
  }

  public String extractUsername(String token) {
    return Jwts.parser().build().parseEncryptedContent(token).toString();

  }

  public String generateToken(String username) {
    return Jwts.builder()
        .subject(username)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
        .signWith(getSigningKey())
        .compact();
  }

  private Key getSigningKey() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

}
