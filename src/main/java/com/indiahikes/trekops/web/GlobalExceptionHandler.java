package com.indiahikes.trekops.web;

import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every exception into an {@link ApiError}, so clients always get the same shape. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ApiError> handleBadSort(
      PropertyReferenceException ex, HttpServletRequest request) {
    return build(
        HttpStatus.BAD_REQUEST, "Unknown sort property: " + ex.getPropertyName(), request, null);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
    return build(HttpStatus.BAD_REQUEST, "Malformed request", request, null);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(
      BadCredentialsException ex, HttpServletRequest request) {
    return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return build(HttpStatus.FORBIDDEN, "Access denied", request, null);
  }

  @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
  public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
    String message =
        ex instanceof ResourceNotFoundException ? ex.getMessage() : "Resource not found";
    return build(HttpStatus.NOT_FOUND, message, request, null);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiError> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    return build(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed", request, null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(
      BusinessRuleException ex, HttpServletRequest request) {
    return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
  }

  /** Last resort. The client sees no internals; the details go to the log. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
    return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null);
  }

  private ResponseEntity<ApiError> build(
      HttpStatus status, String message, HttpServletRequest request, Map<String, String> errors) {
    ApiError body = ApiError.of(clock, status, message, request.getRequestURI(), errors);
    return ResponseEntity.status(status).body(body);
  }
}
