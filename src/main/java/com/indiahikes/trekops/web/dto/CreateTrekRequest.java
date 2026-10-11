package com.indiahikes.trekops.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateTrekRequest(
    @NotBlank @Size(max = 255) String name, @NotNull Instant startAt, @NotNull Instant endAt) {

  /** Reported as the field "endAfterStart" when it fails. */
  @JsonIgnore
  @AssertTrue(message = "endAt must be after startAt")
  public boolean isEndAfterStart() {
    return startAt == null || endAt == null || endAt.isAfter(startAt);
  }
}
