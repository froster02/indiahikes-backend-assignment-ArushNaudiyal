package com.indiahikes.trekops.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegisterParticipantRequest(
    @NotNull UUID trekId,
    @NotBlank @Size(max = 255) String name,
    @NotBlank
        @Pattern(
            regexp = "\\+?[0-9 ()-]{7,20}",
            message = "must be a phone number of 7 to 20 digits, spaces, brackets or dashes")
        String phone,
    @NotBlank @Size(max = 255) String emergencyContact) {}
