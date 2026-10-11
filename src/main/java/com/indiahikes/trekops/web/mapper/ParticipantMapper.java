package com.indiahikes.trekops.web.mapper;

import com.indiahikes.trekops.domain.CheckIn;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.web.dto.CheckInResponse;
import com.indiahikes.trekops.web.dto.ParticipantDetailResponse;
import com.indiahikes.trekops.web.dto.ParticipantResponse;
import java.util.List;

/** Entity to DTO conversion for participants and check-ins. Call inside a transaction. */
public final class ParticipantMapper {

  private ParticipantMapper() {}

  public static ParticipantResponse toResponse(Participant participant) {
    return new ParticipantResponse(
        participant.getId(),
        participant.getTrek().getId(),
        participant.getTrek().getName(),
        participant.getName(),
        participant.getPhone(),
        participant.getEmergencyContact(),
        participant.getStatus(),
        participant.getCreatedAt(),
        participant.getCreatedBy(),
        participant.getUpdatedAt(),
        participant.getUpdatedBy());
  }

  public static CheckInResponse toResponse(CheckIn checkIn) {
    return new CheckInResponse(
        checkIn.getId(),
        checkIn.getParticipant().getId(),
        checkIn.getCheckpoint().getId(),
        checkIn.getCheckpoint().getName(),
        checkIn.getCheckpoint().getSequenceNo(),
        checkIn.getCheckedInAt(),
        checkIn.getRecordedBy().getUsername());
  }

  public static ParticipantDetailResponse toDetail(
      Participant participant, List<CheckIn> checkIns) {
    return new ParticipantDetailResponse(
        toResponse(participant), checkIns.stream().map(ParticipantMapper::toResponse).toList());
  }
}
