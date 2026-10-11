package com.indiahikes.trekops.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings from {@code app.security.*}. Validation runs at startup, so a missing or short
 * secret stops the app instead of failing on the first login.
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record JwtProperties(
    // HS256 needs a key of at least 32 bytes.
    @NotBlank @Size(min = 32) String jwtSecret, @Min(1) long tokenTtlMinutes) {}
