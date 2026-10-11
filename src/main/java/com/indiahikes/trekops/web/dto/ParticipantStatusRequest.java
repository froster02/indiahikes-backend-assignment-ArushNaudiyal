package com.indiahikes.trekops.web.dto;

import com.indiahikes.trekops.domain.ParticipantStatus;
import jakarta.validation.constraints.NotNull;

public record ParticipantStatusRequest(@NotNull ParticipantStatus status) {}
