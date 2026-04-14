package com.acedigital.meal_plan_manager.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.acedigital.meal_plan_manager.user.DuplicateUserException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

/**
 * Direct unit tests for every {@code @ExceptionHandler} in
 * {@link GlobalExceptionHandler}. The handlers are pure functions of their
 * input exception, so they're tested without bringing up the MVC stack.
 */
public class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void handleValidation_emitsBadRequestWithFieldNames() {
    BeanPropertyBindingResult br = new BeanPropertyBindingResult(new Object(), "obj");
    br.addError(new FieldError("obj", "title", "must not be blank"));
    br.addError(new FieldError("obj", "title", "duplicate field — must dedupe"));
    br.addError(new FieldError("obj", "prepTime", "must be >= 0"));
    MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, br);

    ProblemDetail pd = handler.handleValidation(ex);

    assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
    @SuppressWarnings("unchecked")
    var fields = (java.util.List<String>) pd.getProperties().get("fields");
    // Must have de-duplicated "title".
    assertEquals(2, fields.size());
  }

  @Test
  void handleConstraintViolation_emitsBadRequest() {
    ProblemDetail pd = handler.handleConstraintViolation(
        new ConstraintViolationException("bad", java.util.Set.of()));

    assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
    assertEquals("Validation failed", pd.getTitle());
  }

  @Test
  void handleDuplicateUser_emitsConflict() {
    ProblemDetail pd = handler.handleDuplicateUser(new DuplicateUserException("dup"));
    assertEquals(HttpStatus.CONFLICT.value(), pd.getStatus());
    assertEquals("Conflict", pd.getTitle());
  }

  @Test
  void handleNotFound_emitsNotFound() {
    ProblemDetail pd = handler.handleNotFound(new EntityNotFoundException("missing"));
    assertEquals(HttpStatus.NOT_FOUND.value(), pd.getStatus());
  }

  @Test
  void handleAuth_emitsUnauthorized() {
    ProblemDetail pd = handler.handleAuth(new BadCredentialsException("bad"));
    assertEquals(HttpStatus.UNAUTHORIZED.value(), pd.getStatus());
  }

  @Test
  void handleAccessDenied_returnsNotFoundToAvoidOwnershipLeak() {
    ProblemDetail pd = handler.handleAccessDenied(new AccessDeniedException("nope"));
    assertEquals(HttpStatus.NOT_FOUND.value(), pd.getStatus());
  }

  @Test
  void handleResponseStatus_passesThroughStatusAndReason() {
    ResponseStatusException ex = new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "specific reason");
    ProblemDetail pd = handler.handleResponseStatus(ex);

    assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
    assertEquals("specific reason", pd.getDetail());
  }

  @Test
  void handleResponseStatus_omitsDetailWhenReasonNull() {
    // Mock so we can return a null reason — the real ResponseStatusException
    // constructors fill the message field, which getReason() then returns.
    ResponseStatusException ex = mock(ResponseStatusException.class);
    when(ex.getStatusCode()).thenReturn(HttpStatus.I_AM_A_TEAPOT);
    when(ex.getReason()).thenReturn(null);

    ProblemDetail pd = handler.handleResponseStatus(ex);

    assertEquals(HttpStatus.I_AM_A_TEAPOT.value(), pd.getStatus());
    assertNull(pd.getDetail());
  }

  @Test
  void handleFallback_emits500WithoutLeakingMessage() {
    ProblemDetail pd = handler.handleFallback(new RuntimeException("oops with PII"));
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), pd.getStatus());
    assertEquals("Internal server error", pd.getTitle());
  }
}
