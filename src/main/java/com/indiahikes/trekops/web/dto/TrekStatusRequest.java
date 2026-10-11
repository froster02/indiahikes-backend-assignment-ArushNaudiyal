package com.indiahikes.trekops.web.dto;

import com.indiahikes.trekops.domain.TrekStatus;
import jakarta.validation.constraints.NotNull;

public record TrekStatusRequest(@NotNull TrekStatus status) {}
