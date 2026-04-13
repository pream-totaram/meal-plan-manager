package com.acedigital.meal_plan_manager.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.acedigital.meal_plan_manager.user.DuplicateUserException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * Centralizes error responses so that arbitrary exception messages never
 * leak to clients. Each handler returns a minimal {@link ProblemDetail}
 * while the full exception is only written to server-side logs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");
    // Field names only — never echo user-provided values.
    pd.setDetail("One or more fields are invalid");
    pd.setProperty("fields", ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField())
        .distinct()
        .toList());
    return pd;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Validation failed");
    pd.setDetail("One or more fields are invalid");
    return pd;
  }

  @ExceptionHandler(DuplicateUserException.class)
  public ProblemDetail handleDuplicateUser(DuplicateUserException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflict");
    pd.setDetail("Resource already exists");
    return pd;
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleNotFound(EntityNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Not found");
    return pd;
  }

  @ExceptionHandler({ AuthenticationException.class, BadCredentialsException.class })
  public ProblemDetail handleAuth(AuthenticationException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    pd.setTitle("Unauthorized");
    return pd;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    // Return 404 instead of 403 on ownership failures to avoid leaking
    // which ids exist in the database.
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Not found");
    return pd;
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleResponseStatus(ResponseStatusException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(ex.getStatusCode());
    pd.setTitle(ex.getStatusCode().toString());
    // Only expose reason if it's a code-supplied constant string.
    if (ex.getReason() != null) {
      pd.setDetail(ex.getReason());
    }
    return pd;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleFallback(Exception ex) {
    // Log the real exception server-side; return a sanitized 500 to the client.
    log.error("Unhandled exception", ex);
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setTitle("Internal server error");
    return pd;
  }
}
