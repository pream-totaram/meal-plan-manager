package com.acedigital.meal_plan_manager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Branch-by-branch tests for {@link JwtAuthenticationFilter}. The filter
 * has nested conditionals around the Authorization header, token parsing,
 * existing security context, and the success/failure of token validation;
 * each test isolates one branch.
 */
@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

  @Mock
  private JwtService jwtService;

  @Mock
  private UserDetailsService userDetailsService;

  @Mock
  private FilterChain filterChain;

  private JwtAuthenticationFilter filter;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void noAuthorizationHeader_passesChainWithoutTouchingJwt() throws Exception {
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void nonBearerHeader_passesChainWithoutTouchingJwt() throws Exception {
    request.addHeader("Authorization", "Basic abc");

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void invalidToken_extractUsernameNull_passesChainUnauthenticated() throws Exception {
    request.addHeader("Authorization", "Bearer garbage");
    when(jwtService.extractUsername("garbage")).thenReturn(null);

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(1)).doFilter(request, response);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void validToken_authenticatesAndContinuesChain() throws Exception {
    request.addHeader("Authorization", "Bearer good");
    UserDetails userDetails = new User("alice", "x", List.of(() -> "ROLE_USER"));
    when(jwtService.extractUsername("good")).thenReturn("alice");
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
    when(jwtService.isTokenValid("good", userDetails)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(auth);
    assertEquals("alice", ((UserDetails) auth.getPrincipal()).getUsername());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void existingAuthentication_isNotReplaced() throws Exception {
    request.addHeader("Authorization", "Bearer good");
    when(jwtService.extractUsername("good")).thenReturn("alice");

    UserDetails preExisting = new User("bob", "x", List.of(() -> "ROLE_USER"));
    var preauth = new UsernamePasswordAuthenticationToken(
        preExisting, null, preExisting.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(preauth);

    filter.doFilterInternal(request, response, filterChain);

    // The existing principal must remain — the filter must not call
    // userDetailsService when a principal is already set.
    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertEquals("bob", ((UserDetails) auth.getPrincipal()).getUsername());
    verify(userDetailsService, times(0)).loadUserByUsername(any());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void unknownUser_swallowsExceptionAndContinuesChain() throws Exception {
    request.addHeader("Authorization", "Bearer good");
    when(jwtService.extractUsername("good")).thenReturn("ghost");
    when(userDetailsService.loadUserByUsername("ghost"))
        .thenThrow(new UsernameNotFoundException("nope"));

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void tokenInvalidAgainstUserDetails_doesNotAuthenticate() throws Exception {
    request.addHeader("Authorization", "Bearer good");
    UserDetails userDetails = new User("alice", "x", List.of(() -> "ROLE_USER"));
    when(jwtService.extractUsername("good")).thenReturn("alice");
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);
    when(jwtService.isTokenValid("good", userDetails)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
