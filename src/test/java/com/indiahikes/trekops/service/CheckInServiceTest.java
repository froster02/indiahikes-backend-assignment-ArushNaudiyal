package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indiahikes.trekops.domain.CheckIn;
import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.domain.UserRole;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.UserRepository;
import com.indiahikes.trekops.web.dto.CheckInResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

class CheckInServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-10T10:00:00Z");

  private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
  private final ParticipantRepository participantRepository = mock(ParticipantRepository.class);
  private final CheckpointRepository checkpointRepository = mock(CheckpointRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final CheckInService service =
      new CheckInService(
          checkInRepository, participantRepository, checkpointRepository, userRepository, clock);

  private final UUID participantId = UUID.randomUUID();
  private final UUID checkpointId = UUID.randomUUID();
  private Trek trek;
  private Participant participant;
  private Checkpoint checkpoint;

  @BeforeEach
  void setUp() {
    trek =
        new Trek(
            "Kedarkantha Trek",
            Instant.parse("2026-08-10T00:30:00Z"),
            Instant.parse("2026-08-13T12:30:00Z"));
    trek.setStatus(TrekStatus.ACTIVE);
    participant = new Participant(trek, "Aarav Sharma", "+91 90000 00001", "+91 90000 10001");
    participant.setStatus(ParticipantStatus.ON_TREK);
    checkpoint = new Checkpoint(trek, "Sankri", 2, 8);
    // The database generates ids, so unit tests set them by hand.
    ReflectionTestUtils.setField(trek, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(participant, "id", participantId);
    ReflectionTestUtils.setField(checkpoint, "id", checkpointId);
    User ops = new User("ops", "hash", UserRole.OPERATIONS);

    when(userRepository.findByUsername("ops")).thenReturn(Optional.of(ops));
    when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
    when(checkpointRepository.findById(checkpointId)).thenReturn(Optional.of(checkpoint));
    when(checkInRepository.saveAndFlush(any(CheckIn.class))).thenAnswer(c -> c.getArgument(0));
  }

  @Test
  void validCheckInIsSavedWithTheGivenTimeAndRecorder() {
    Instant arrived = Instant.parse("2026-08-10T08:45:00Z");

    CheckInResponse response = service.record(participantId, checkpointId, arrived, "ops");

    assertThat(response.checkedInAt()).isEqualTo(arrived);
    assertThat(response.recordedBy()).isEqualTo("ops");
    assertThat(response.checkpointName()).isEqualTo("Sankri");
    verify(checkInRepository).saveAndFlush(any(CheckIn.class));
  }

  @Test
  void missingTimeDefaultsToNow() {
    assertThat(service.record(participantId, checkpointId, null, "ops").checkedInAt())
        .isEqualTo(NOW);
  }

  @Test
  void aRegisteredParticipantMayCheckIn() {
    participant.setStatus(ParticipantStatus.REGISTERED);

    assertThat(service.record(participantId, checkpointId, null, "ops").participantId())
        .isEqualTo(participantId);
  }

  @Test
  void checkpointFromAnotherTrekIsRejected() {
    Trek other = new Trek("Other Trek", NOW, NOW.plusSeconds(86400));
    other.setStatus(TrekStatus.ACTIVE);
    ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
    checkpoint = new Checkpoint(other, "Elsewhere", 1, 0);
    when(checkpointRepository.findById(checkpointId)).thenReturn(Optional.of(checkpoint));

    assertThatThrownBy(() -> service.record(participantId, checkpointId, null, "ops"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Checkpoint does not belong to the participant's trek");
    verify(checkInRepository, never()).saveAndFlush(any());
  }

  @Test
  void duplicateCheckInIsAConflict() {
    when(checkInRepository.existsByParticipantIdAndCheckpointId(participantId, checkpointId))
        .thenReturn(true);

    assertThatThrownBy(() -> service.record(participantId, checkpointId, null, "ops"))
        .isInstanceOf(ConflictException.class);
    verify(checkInRepository, never()).saveAndFlush(any());
  }

  @Test
  void aRaceThatHitsTheUniqueConstraintIsAlsoAConflict() {
    when(checkInRepository.saveAndFlush(any(CheckIn.class)))
        .thenThrow(new DataIntegrityViolationException("uq_check_ins_participant_checkpoint"));

    assertThatThrownBy(() -> service.record(participantId, checkpointId, null, "ops"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void futureTimeIsRejected() {
    Instant future = NOW.plusSeconds(60);

    assertThatThrownBy(() -> service.record(participantId, checkpointId, future, "ops"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Check-in time cannot be in the future");
  }

  @Test
  void trekThatIsNotActiveIsRejected() {
    trek.setStatus(TrekStatus.PLANNED);

    assertThatThrownBy(() -> service.record(participantId, checkpointId, null, "ops"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ACTIVE");
  }

  @Test
  void droppedParticipantIsRejected() {
    participant.setStatus(ParticipantStatus.DROPPED);

    assertThatThrownBy(() -> service.record(participantId, checkpointId, null, "ops"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("DROPPED");
  }

  @Test
  void unknownParticipantOrCheckpointIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(participantRepository.findById(unknown)).thenReturn(Optional.empty());
    when(checkpointRepository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.record(unknown, checkpointId, null, "ops"))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.record(participantId, unknown, null, "ops"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void tokenOfAUserWhoNoLongerExistsIsRejected() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.record(participantId, checkpointId, null, "ghost"))
        .isInstanceOf(BadCredentialsException.class);
  }
}
