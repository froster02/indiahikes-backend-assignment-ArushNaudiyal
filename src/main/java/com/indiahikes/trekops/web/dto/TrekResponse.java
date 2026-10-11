package com.indiahikes.trekops.web.dto;

import com.indiahikes.trekops.domain.TrekStatus;
import java.time.Instant;
import java.util.UUID;

public record TrekResponse(
    UUID id,
    String name,
    Instant startAt,
    Instant endAt,
    TrekStatus status,
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy) {}
