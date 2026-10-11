package com.indiahikes.trekops.web.dto;

import com.indiahikes.trekops.domain.AlertStatus;
import java.time.Instant;
import java.util.UUID;

/** {@code expectedAt} is when the participant should have reported at the checkpoint. */
public record AlertResponse(
    UUID id,
    AlertStatus status,
    UUID participantId,
    String participantName,
    UUID trekId,
    String trekName,
    UUID checkpointId,
    String checkpointName,
    int sequenceNo,
    Instant expectedAt,
    Instant createdAt,
    String acknowledgedBy,
    Instant acknowledgedAt,
    String resolvedBy,
    Instant resolvedAt) {}
