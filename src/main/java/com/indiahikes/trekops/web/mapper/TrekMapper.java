package com.indiahikes.trekops.web.mapper;

import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.web.dto.CheckpointResponse;
import com.indiahikes.trekops.web.dto.TrekDetailResponse;
import com.indiahikes.trekops.web.dto.TrekResponse;
import java.util.List;

/** Entity to DTO conversion for treks and checkpoints. Hand written to keep it explicit. */
public final class TrekMapper {

  private TrekMapper() {}

  public static TrekResponse toResponse(Trek trek) {
    return new TrekResponse(
        trek.getId(),
        trek.getName(),
        trek.getStartAt(),
        trek.getEndAt(),
        trek.getStatus(),
        trek.getCreatedAt(),
        trek.getCreatedBy(),
        trek.getUpdatedAt(),
        trek.getUpdatedBy());
  }

  public static CheckpointResponse toResponse(Checkpoint checkpoint) {
    return new CheckpointResponse(
        checkpoint.getId(),
        checkpoint.getSequenceNo(),
        checkpoint.getName(),
        checkpoint.getExpectedAfterHours());
  }

  public static TrekDetailResponse toDetail(Trek trek, List<Checkpoint> checkpoints) {
    return new TrekDetailResponse(
        toResponse(trek), checkpoints.stream().map(TrekMapper::toResponse).toList());
  }
}
