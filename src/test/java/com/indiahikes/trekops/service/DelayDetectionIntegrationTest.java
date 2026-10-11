package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.DelayCheckProperties;
import com.indiahikes.trekops.config.JpaAuditingConfig;
import com.indiahikes.trekops.config.SecurityAuditorAware;
import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.CheckIn;
import com.indiahikes.trekops.repository.AlertRepository;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.TrekRepository;
import com.indiahikes.trekops.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * The delay rule as real SQL on H2 against the seed data. The Kedarkantha trek starts at 2026-08-10
 * 00:30 UTC. At 10:00 UTC, Dehradun (0 h) and Sankri (8 h) are overdue. Aarav and Diya are ON_TREK;
 * Rohan is only REGISTERED.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, ClockConfig.class, SecurityAuditorAware.class})
class DelayDetectionIntegrationTest {

  private static final Instant TEN_AM = Instant.parse("2026-08-10T10:00:00Z");
  private static final UUID AARAV = UUID.fromString("d0000000-0000-0000-0000-000000000001");
  private static final UUID DEHRADUN = UUID.fromString("c0000000-0000-0000-0000-000000000001");

  @Autowired private TrekRepository trekRepository;
  @Autowired private CheckpointRepository checkpointRepository;
  @Autowired private ParticipantRepository participantRepository;
  @Autowired private CheckInRepository checkInRepository;
  @Autowired private AlertRepository alertRepository;
  @Autowired private UserRepository userRepository;

  private DelayDetectionService serviceAt(Instant now) {
    return new DelayDetectionService(
        trekRepository,
        checkpointRepository,
        participantRepository,
        checkInRepository,
        alertRepository,
        new DelayCheckProperties(true, 60000, 0),
        event -> {},
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void raisesOneAlertPerOverdueCheckpointForEachParticipantOnTrek() {
    List<Alert> created = serviceAt(TEN_AM).detectDelays();

    assertThat(created)
        .extracting(a -> a.getParticipant().getName() + " @ " + a.getCheckpoint().getName())
        .containsExactlyInAnyOrder(
            "Aarav Sharma @ Dehradun",
            "Aarav Sharma @ Sankri",
            "Diya Mehta @ Dehradun",
            "Diya Mehta @ Sankri");
    assertThat(alertRepository.count()).isEqualTo(4);
  }

  @Test
  void runningAgainCreatesNoDuplicates() {
    serviceAt(TEN_AM).detectDelays();

    assertThat(serviceAt(TEN_AM).detectDelays()).isEmpty();
    assertThat(alertRepository.count()).isEqualTo(4);
  }

  @Test
  void aCheckInPreventsTheAlert() {
    checkInRepository.saveAndFlush(
        new CheckIn(
            participantRepository.findById(AARAV).orElseThrow(),
            checkpointRepository.findById(DEHRADUN).orElseThrow(),
            Instant.parse("2026-08-10T00:20:00Z"),
            userRepository.findByUsername("ops").orElseThrow()));

    assertThat(serviceAt(TEN_AM).detectDelays())
        .extracting(a -> a.getParticipant().getName() + " @ " + a.getCheckpoint().getName())
        .doesNotContain("Aarav Sharma @ Dehradun")
        .hasSize(3);
  }

  @Test
  void laterCheckpointsAlertAsTimePasses() {
    serviceAt(TEN_AM).detectDelays();

    // Juda Ka Talab is due 28 hours after the start.
    List<Alert> nextDay = serviceAt(Instant.parse("2026-08-11T05:00:00Z")).detectDelays();

    assertThat(nextDay)
        .extracting(a -> a.getCheckpoint().getName())
        .containsExactlyInAnyOrder("Juda Ka Talab", "Juda Ka Talab");
  }
}
