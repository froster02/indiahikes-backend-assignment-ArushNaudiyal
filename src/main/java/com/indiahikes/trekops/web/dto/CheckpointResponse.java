package com.indiahikes.trekops.web.dto;

import java.util.UUID;

public record CheckpointResponse(UUID id, int sequenceNo, String name, int expectedAfterHours) {}
