package com.indiahikes.trekops.web.dto;

import com.indiahikes.trekops.domain.ParticipantStatus;
import java.time.Instant;
import java.util.UUID;

public record ParticipantResponse(
    UUID id,
    UUID trekId,
    String trekName,
    String name,
    String phone,
    String emergencyContact,
    ParticipantStatus status,
    Instant createdAt,
    String createdBy,
    Instant updatedAt,
    String updatedBy) {}
