package com.indiahikes.trekops.service;

import com.indiahikes.trekops.config.DelayCheckProperties;
import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.notification.AlertCreatedEvent;
import com.indiahikes.trekops.repository.AlertRepository;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.TrekRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finds delayed participants and raises alerts.
 *
 * <p>A participant is delayed at a checkpoint when the trek is ACTIVE, the participant is ON_TREK,
 * {@code trek.startAt + checkpoint.expectedAfterHours + grace} has passed, and the participant has
 * no check-in there. A pair that already has an alert, in any status, gets no new one, so running
 * the check again never creates duplicates and a resolved alert is not raised again.
 *
 * <p>Each new alert publishes an {@link AlertCreatedEvent} for the notification channels.
 */
@Service
public class DelayDetectionService {

  private static final Logger log = LoggerFactory.getLogger(DelayDetectionService.class);

  private final TrekRepository trekRepository;
  private final CheckpointRepository checkpointRepository;
  private final ParticipantRepository participantRepository;
  private final CheckInRepository checkInRepository;
  private final AlertRepository alertRepository;
  private final ApplicationEventPublisher events;
  private final Duration grace;
  private final Clock clock;

  public DelayDetectionService(
      TrekRepository trekRepository,
      CheckpointRepository checkpointRepository,
      ParticipantRepository participantRepository,
      CheckInRepository checkInRepository,
      AlertRepository alertRepository,
      DelayCheckProperties properties,
      ApplicationEventPublisher events,
      Clock clock) {
    this.trekRepository = trekRepository;
    this.checkpointRepository = checkpointRepository;
    this.participantRepository = participantRepository;
    this.checkInRepository = checkInRepository;
    this.alertRepository = alertRepository;
    this.events = events;
    this.grace = Duration.ofMinutes(properties.graceMinutes());
    this.clock = clock;
  }

  /**
   * Runs one check over all active treks in one transaction.
   *
   * @return the alerts created by this run
   */
  @Transactional
  public List<Alert> detectDelays() {
    Instant now = clock.instant();
    List<Alert> created = new ArrayList<>();
    for (Trek trek : trekRepository.findByStatus(TrekStatus.ACTIVE)) {
      if (trek.getStartAt().isAfter(now)) {
        continue;
      }
      List<Checkpoint> overdue =
          checkpointRepository.findByTrekIdOrderBySequenceNo(trek.getId()).stream()
              .filter(checkpoint -> isOverdue(trek, checkpoint, now))
              .toList();
      if (overdue.isEmpty()) {
        continue;
      }
      List<Participant> onTrek =
          participantRepository.findByTrekIdAndStatus(trek.getId(), ParticipantStatus.ON_TREK);
      for (Participant participant : onTrek) {
        for (Checkpoint checkpoint : overdue) {
          if (needsAlert(participant, checkpoint)) {
            Alert alert = alertRepository.save(new Alert(participant, checkpoint));
            created.add(alert);
            events.publishEvent(toEvent(alert, trek, participant, checkpoint));
          }
        }
      }
    }
    if (!created.isEmpty()) {
      log.info("Delay check raised {} alert(s)", created.size());
    }
    return created;
  }

  private boolean isOverdue(Trek trek, Checkpoint checkpoint, Instant now) {
    return now.isAfter(expectedAt(trek, checkpoint).plus(grace));
  }

  private static Instant expectedAt(Trek trek, Checkpoint checkpoint) {
    return trek.getStartAt().plus(Duration.ofHours(checkpoint.getExpectedAfterHours()));
  }

  private static AlertCreatedEvent toEvent(
      Alert alert, Trek trek, Participant participant, Checkpoint checkpoint) {
    return new AlertCreatedEvent(
        alert.getId(),
        participant.getName(),
        participant.getPhone(),
        trek.getName(),
        checkpoint.getName(),
        expectedAt(trek, checkpoint),
        alert.getCreatedAt());
  }

  private boolean needsAlert(Participant participant, Checkpoint checkpoint) {
    boolean checkedIn =
        checkInRepository.existsByParticipantIdAndCheckpointId(
            participant.getId(), checkpoint.getId());
    boolean alreadyAlerted =
        alertRepository.existsByParticipantIdAndCheckpointId(
            participant.getId(), checkpoint.getId());
    return !checkedIn && !alreadyAlerted;
  }
}
