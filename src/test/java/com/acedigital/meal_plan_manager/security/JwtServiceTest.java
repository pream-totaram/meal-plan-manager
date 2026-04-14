package com.acedigital.meal_plan_manager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Direct unit tests for {@link JwtService} — every constructor branch and
 * every parse / validate code path including expired and tampered tokens.
 */
public class JwtServiceTest {

  // 32 bytes once base64-decoded — minimum for HS256.
  private static final String VALID_SECRET = "47MOYk+KUBPcWDpJjoQ5ttBUvsb1E6+cDG55GEjpoPI=";
  private static final long ONE_HOUR = 3_600_000L;

  private final UserDetails alice = new User("alice", "x",
      List.of(() -> "ROLE_USER"));

  // ---- constructor --------------------------------------------------------

  @Test
  void constructor_rejectsNullSecret() {
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> new JwtService(null, ONE_HOUR));
    assertTrue(ex.getMessage().contains("not configured"));
  }

  @Test
  void constructor_rejectsBlankSecret() {
    assertThrows(IllegalStateException.class,
        () -> new JwtService("   ", ONE_HOUR));
  }

  @Test
  void constructor_rejectsShortSecret() {
    // 16-byte key — HS256 requires 32.
    String shortKey = io.jsonwebtoken.io.Encoders.BASE64.encode(new byte[16]);
    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> new JwtService(shortKey, ONE_HOUR));
    assertTrue(ex.getMessage().contains("32 bytes"));
  }

  // ---- generateToken / extractUsername / isTokenValid ---------------------

  @Test
  void generateThenExtract_roundTripsSubject() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    String token = service.generateToken(alice);
    assertNotNull(token);
    assertEquals("alice", service.extractUsername(token));
    assertTrue(service.isTokenValid(token, alice));
  }

  @Test
  void extractUsername_returnsNullForGarbage() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    assertNull(service.extractUsername("not-a-jwt"));
  }

  @Test
  void extractUsername_returnsNullForTokenSignedWithDifferentKey() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    // A token signed by a totally different key — signature must reject.
    String foreignKey = "Z8YJj2zL3uF2nQpRtBwM5xKvO9Y0sP1aD6cE7fG8hI4=";
    JwtService foreign = new JwtService(foreignKey, ONE_HOUR);
    String token = foreign.generateToken(alice);
    assertNull(service.extractUsername(token));
  }

  @Test
  void isTokenValid_falseWhenSubjectMismatches() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    String token = service.generateToken(alice);
    UserDetails bob = new User("bob", "x", List.of(() -> "ROLE_USER"));
    assertFalse(service.isTokenValid(token, bob));
  }

  @Test
  void isTokenValid_falseWhenExpired() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    // Hand-craft an expired token so we exercise the "exp.after(now)==false"
    // branch deterministically without sleeping.
    long past = System.currentTimeMillis() - 10_000L;
    String expired = Jwts.builder()
        .subject("alice")
        .issuedAt(new Date(past - ONE_HOUR))
        .expiration(new Date(past))
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(VALID_SECRET)),
            Jwts.SIG.HS256)
        .compact();

    // Expired tokens make jjwt throw inside parseClaims, so isTokenValid
    // returns false via the catch — and so does extractUsername.
    assertFalse(service.isTokenValid(expired, alice));
    assertNull(service.extractUsername(expired));
  }

  @Test
  void isTokenValid_falseForGarbage() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    assertFalse(service.isTokenValid("nope", alice));
  }

  @Test
  void isTokenValid_falseWhenSubjectMissing() {
    JwtService service = new JwtService(VALID_SECRET, ONE_HOUR);
    // Token with NO subject claim — exercises the subject==null branch.
    String token = Jwts.builder()
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + ONE_HOUR))
        .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(VALID_SECRET)),
            Jwts.SIG.HS256)
        .compact();
    assertFalse(service.isTokenValid(token, alice));
  }
}
