package com.indiahikes.trekops.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/** {@code checkedInAt} is optional and defaults to the current time. */
public record RecordCheckInRequest(
    @NotNull UUID participantId, @NotNull UUID checkpointId, Instant checkedInAt) {}
