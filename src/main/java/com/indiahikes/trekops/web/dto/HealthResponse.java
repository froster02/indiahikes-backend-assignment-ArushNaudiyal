package com.indiahikes.trekops.web.dto;

import java.time.Instant;

/** {@code status} is UP when the database answers, DOWN otherwise. */
public record HealthResponse(
    String status, String application, String version, String database, Instant time) {}
