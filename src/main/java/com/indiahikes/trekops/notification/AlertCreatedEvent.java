package com.indiahikes.trekops.notification;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when the delay check raises an alert. It carries plain values, not entities, so
 * listeners can use it after the transaction has ended.
 */
public record AlertCreatedEvent(
    UUID alertId,
    String participantName,
    String participantPhone,
    String trekName,
    String checkpointName,
    Instant expectedAt,
    Instant createdAt) {}
