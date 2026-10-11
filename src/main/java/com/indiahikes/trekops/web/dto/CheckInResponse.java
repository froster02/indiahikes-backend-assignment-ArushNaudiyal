package com.indiahikes.trekops.web.dto;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
    UUID id,
    UUID participantId,
    UUID checkpointId,
    String checkpointName,
    int sequenceNo,
    Instant checkedInAt,
    String recordedBy) {}
