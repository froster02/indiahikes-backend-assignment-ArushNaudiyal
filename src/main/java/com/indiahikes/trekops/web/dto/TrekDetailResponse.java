package com.indiahikes.trekops.web.dto;

import java.util.List;

/** A trek with its checkpoints in sequence order. */
public record TrekDetailResponse(TrekResponse trek, List<CheckpointResponse> checkpoints) {}
