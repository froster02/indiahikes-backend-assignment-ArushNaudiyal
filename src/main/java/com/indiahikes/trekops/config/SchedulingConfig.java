package com.indiahikes.trekops.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on Spring's @Scheduled support. Jobs run on a single scheduler thread by default. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
