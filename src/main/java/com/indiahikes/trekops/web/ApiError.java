package com.indiahikes.trekops.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * The one error body every endpoint returns. {@code errors} is set only for validation failures and
 * maps each bad field to its message.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> errors) {

  public static ApiError of(Clock clock, HttpStatus status, String message, String path) {
    return new ApiError(
        clock.instant(), status.value(), status.getReasonPhrase(), message, path, null);
  }

  public static ApiError of(
      Clock clock, HttpStatus status, String message, String path, Map<String, String> errors) {
    return new ApiError(
        clock.instant(), status.value(), status.getReasonPhrase(), message, path, errors);
  }
}
