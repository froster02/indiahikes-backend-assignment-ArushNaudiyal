package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indiahikes.trekops.config.DelayCheckProperties;
import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.repository.AlertRepository;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.TrekRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** The delay rule with a fixed clock. The trek starts at 00:30 UTC; Sankri is due 8 hours later. */
class DelayDetectionServiceTest {

  private static final Instant START = Instant.parse("2026-08-10T00:30:00Z");
  private static final Instant SANKRI_DUE = Instant.parse("2026-08-10T08:30:00Z");

  private final TrekRepository trekRepository = mock(TrekRepository.class);
  private final CheckpointRepository checkpointRepository = mock(CheckpointRepository.class);
  private final ParticipantRepository participantRepository = mock(ParticipantRepository.class);
  private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
  private final AlertRepository alertRepository = mock(AlertRepository.class);

  private Trek trek;
  private Checkpoint sankri;
  private Participant aarav;

  @BeforeEach
  void setUp() {
    trek = new Trek("Kedarkantha Trek", START, Instant.parse("2026-08-13T12:30:00Z"));
    trek.setStatus(TrekStatus.ACTIVE);
    sankri = new Checkpoint(trek, "Sankri", 2, 8);
    aarav = new Participant(trek, "Aarav Sharma", "+91 90000 00001", "x");
    aarav.setStatus(ParticipantStatus.ON_TREK);
    ReflectionTestUtils.setField(trek, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(sankri, "id", UUID.randomUUID());
    ReflectionTestUtils.setField(aarav, "id", UUID.randomUUID());

    when(trekRepository.findByStatus(TrekStatus.ACTIVE)).thenReturn(List.of(trek));
    when(checkpointRepository.findByTrekIdOrderBySequenceNo(trek.getId()))
        .thenReturn(List.of(sankri));
    when(participantRepository.findByTrekIdAndStatus(trek.getId(), ParticipantStatus.ON_TREK))
        .thenReturn(List.of(aarav));
    when(alertRepository.save(any(Alert.class))).thenAnswer(c -> c.getArgument(0));
  }

  private DelayDetectionService serviceAt(Instant now, long graceMinutes) {
    return new DelayDetectionService(
        trekRepository,
        checkpointRepository,
        participantRepository,
        checkInRepository,
        alertRepository,
        new DelayCheckProperties(true, 60000, graceMinutes),
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void overdueWithoutCheckInRaisesAnOpenAlert() {
    List<Alert> created = serviceAt(SANKRI_DUE.plusSeconds(60), 0).detectDelays();

    assertThat(created).hasSize(1);
    assertThat(created.get(0).getParticipant()).isSameAs(aarav);
    assertThat(created.get(0).getCheckpoint()).isSameAs(sankri);
  }

  @Test
  void notYetDueRaisesNothing() {
    assertThat(serviceAt(SANKRI_DUE.minusSeconds(60), 0).detectDelays()).isEmpty();
    verify(alertRepository, never()).save(any());
  }

  @Test
  void exactlyAtTheExpectedTimeIsNotYetLate() {
    assertThat(serviceAt(SANKRI_DUE, 0).detectDelays()).isEmpty();
  }

  @Test
  void graceMinutesDelayTheAlert() {
    assertThat(serviceAt(SANKRI_DUE.plusSeconds(10 * 60), 15).detectDelays()).isEmpty();
    assertThat(serviceAt(SANKRI_DUE.plusSeconds(16 * 60), 15).detectDelays()).hasSize(1);
  }

  @Test
  void checkedInParticipantRaisesNothing() {
    when(checkInRepository.existsByParticipantIdAndCheckpointId(aarav.getId(), sankri.getId()))
        .thenReturn(true);

    assertThat(serviceAt(SANKRI_DUE.plusSeconds(3600), 0).detectDelays()).isEmpty();
  }

  @Test
  void existingAlertIsNotDuplicated() {
    when(alertRepository.existsByParticipantIdAndCheckpointId(aarav.getId(), sankri.getId()))
        .thenReturn(true);

    assertThat(serviceAt(SANKRI_DUE.plusSeconds(3600), 0).detectDelays()).isEmpty();
    verify(alertRepository, never()).save(any());
  }

  @Test
  void trekThatHasNotStartedIsSkipped() {
    Instant beforeStart = START.minusSeconds(60);
    assertThat(serviceAt(beforeStart, 0).detectDelays()).isEmpty();
    verify(checkpointRepository, never()).findByTrekIdOrderBySequenceNo(any());
  }

  @Test
  void onlyActiveTreksAndOnTrekParticipantsAreScanned() {
    serviceAt(SANKRI_DUE.plusSeconds(3600), 0).detectDelays();

    // The queries themselves restrict the scope, so REGISTERED, DROPPED and COMPLETED
    // participants and PLANNED, COMPLETED and CANCELLED treks are never loaded.
    verify(trekRepository).findByStatus(TrekStatus.ACTIVE);
    verify(participantRepository).findByTrekIdAndStatus(trek.getId(), ParticipantStatus.ON_TREK);
  }
}
