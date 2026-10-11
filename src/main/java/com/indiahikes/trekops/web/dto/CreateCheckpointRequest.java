package com.indiahikes.trekops.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateCheckpointRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @Positive Integer sequenceNo,
    @NotNull @PositiveOrZero Integer expectedAfterHours) {}
