package com.indiahikes.trekops.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Delay detection settings from {@code app.scheduler.delay-check.*}.
 *
 * @param enabled turns the scheduled check on or off
 * @param intervalMs pause between the end of one run and the start of the next
 * @param graceMinutes how long after the expected time a participant counts as delayed
 */
@Validated
@ConfigurationProperties(prefix = "app.scheduler.delay-check")
public record DelayCheckProperties(
    boolean enabled, @Min(1000) long intervalMs, @Min(0) long graceMinutes) {}
