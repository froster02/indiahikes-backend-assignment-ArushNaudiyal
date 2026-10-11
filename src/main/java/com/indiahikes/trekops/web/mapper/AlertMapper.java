package com.indiahikes.trekops.web.mapper;

import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.web.dto.AlertResponse;
import java.time.Duration;

/** Entity to DTO conversion for alerts. Call inside a transaction. */
public final class AlertMapper {

  private AlertMapper() {}

  public static AlertResponse toResponse(Alert alert) {
    var participant = alert.getParticipant();
    var trek = participant.getTrek();
    var checkpoint = alert.getCheckpoint();
    return new AlertResponse(
        alert.getId(),
        alert.getStatus(),
        participant.getId(),
        participant.getName(),
        trek.getId(),
        trek.getName(),
        checkpoint.getId(),
        checkpoint.getName(),
        checkpoint.getSequenceNo(),
        trek.getStartAt().plus(Duration.ofHours(checkpoint.getExpectedAfterHours())),
        alert.getCreatedAt(),
        username(alert.getAcknowledgedBy()),
        alert.getAcknowledgedAt(),
        username(alert.getResolvedBy()),
        alert.getResolvedAt());
  }

  private static String username(User user) {
    return user == null ? null : user.getUsername();
  }
}
