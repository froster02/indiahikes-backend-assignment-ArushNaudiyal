package com.indiahikes.trekops.service;

import com.indiahikes.trekops.domain.AlertStatus;
import com.indiahikes.trekops.domain.CheckIn;
import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.AlertRepository;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.UserRepository;
import com.indiahikes.trekops.web.dto.CheckInResponse;
import com.indiahikes.trekops.web.mapper.ParticipantMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records that a participant reported at a checkpoint. */
@Service
public class CheckInService {

  private static final List<AlertStatus> ACTIVE_ALERT =
      List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

  private final CheckInRepository checkInRepository;
  private final AlertRepository alertRepository;
  private final ParticipantRepository participantRepository;
  private final CheckpointRepository checkpointRepository;
  private final UserRepository userRepository;
  private final Clock clock;

  public CheckInService(
      CheckInRepository checkInRepository,
      AlertRepository alertRepository,
      ParticipantRepository participantRepository,
      CheckpointRepository checkpointRepository,
      UserRepository userRepository,
      Clock clock) {
    this.checkInRepository = checkInRepository;
    this.alertRepository = alertRepository;
    this.participantRepository = participantRepository;
    this.checkpointRepository = checkpointRepository;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  /**
   * Rules, in order: both ids exist (404), the checkpoint is on the participant's trek (422), the
   * trek is ACTIVE (422), the participant is not DROPPED or COMPLETED (422), the time is not in the
   * future (422), and the participant has not already checked in there (409). A late check-in also
   * resolves any OPEN or ACKNOWLEDGED alert for the same participant and checkpoint.
   *
   * @param checkedInAt when the participant arrived; the current time when null
   * @param recordedByUsername the logged in user who records this
   */
  @Transactional
  public CheckInResponse record(
      UUID participantId, UUID checkpointId, Instant checkedInAt, String recordedByUsername) {
    User recordedBy =
        userRepository
            .findByUsername(recordedByUsername)
            .orElseThrow(() -> new BadCredentialsException("Unknown user"));
    Participant participant =
        participantRepository
            .findById(participantId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Participant " + participantId + " not found"));
    Checkpoint checkpoint =
        checkpointRepository
            .findById(checkpointId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Checkpoint " + checkpointId + " not found"));

    if (!checkpoint.getTrek().getId().equals(participant.getTrek().getId())) {
      throw new BusinessRuleException("Checkpoint does not belong to the participant's trek");
    }
    if (participant.getTrek().getStatus() != TrekStatus.ACTIVE) {
      throw new BusinessRuleException("Check-ins are accepted only while the trek is ACTIVE");
    }
    if (participant.getStatus() == ParticipantStatus.DROPPED
        || participant.getStatus() == ParticipantStatus.COMPLETED) {
      throw new BusinessRuleException(
          "Participant is " + participant.getStatus() + " and cannot check in");
    }
    Instant now = clock.instant();
    Instant time = checkedInAt != null ? checkedInAt : now;
    if (time.isAfter(now)) {
      throw new BusinessRuleException("Check-in time cannot be in the future");
    }
    if (checkInRepository.existsByParticipantIdAndCheckpointId(participantId, checkpointId)) {
      throw new ConflictException("Participant has already checked in at this checkpoint");
    }

    try {
      CheckIn saved =
          checkInRepository.saveAndFlush(new CheckIn(participant, checkpoint, time, recordedBy));
      // The participant has now reported, so the delay alert is no longer needed.
      alertRepository
          .findByParticipantIdAndCheckpointIdAndStatusIn(participantId, checkpointId, ACTIVE_ALERT)
          .forEach(alert -> alert.resolve(recordedBy, now));
      return ParticipantMapper.toResponse(saved);
    } catch (DataIntegrityViolationException e) {
      // Two requests passed the check above at the same moment. The unique constraint stops the
      // second one.
      throw new ConflictException("Participant has already checked in at this checkpoint");
    }
  }
}
