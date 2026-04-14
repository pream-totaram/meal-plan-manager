package com.acedigital.meal_plan_manager.security;

import java.util.Date;

import javax.crypto.SecretKey;


import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtService {

  private final SecretKey signingKey;
  private final long jwtExpirationMs;

  public JwtService(String jwtSecret, long jwtExpirationMs) {
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException(
          "application.security.jwt.secret-key is not configured. "
              + "Set the JWT_SECRET environment variable to a base64-encoded "
              + "256-bit (or larger) random key.");
    }
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    if (keyBytes.length < 32) {
      throw new IllegalStateException(
          "JWT secret must decode to at least 32 bytes (256 bits) for HS256");
    }
    this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    this.jwtExpirationMs = jwtExpirationMs;
  }

  public String generateToken(UserDetails user) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .subject(user.getUsername())
        .issuedAt(new Date(now))
        .expiration(new Date(now + jwtExpirationMs))
        .signWith(signingKey, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Parses and verifies the JWT's signature and expiration. Any failure
   * (tampered signature, expired, malformed, wrong algorithm, etc.) is
   * treated as "invalid" and returns {@code null} — callers must not
   * authenticate when the result is null.
   */
  public String extractUsername(String token) {
    try {
      Claims claims = parseClaims(token);
      return claims.getSubject();
    } catch (JwtException | IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Full validation: signature + expiration + subject must match the
   * supplied {@link UserDetails}.
   */
  public boolean isTokenValid(String token, UserDetails userDetails) {
    try {
      // parseClaims itself throws ExpiredJwtException for expired tokens,
      // so reaching the subject comparison already implies a live token.
      Claims claims = parseClaims(token);
      String subject = claims.getSubject();
      return subject != null && subject.equals(userDetails.getUsername());
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
