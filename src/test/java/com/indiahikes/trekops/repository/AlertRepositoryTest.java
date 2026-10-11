package com.indiahikes.trekops.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.JpaAuditingConfig;
import com.indiahikes.trekops.config.SecurityAuditorAware;
import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.AlertStatus;
import com.indiahikes.trekops.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/** Alert queries on H2 with the seed data. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, ClockConfig.class, SecurityAuditorAware.class})
class AlertRepositoryTest {

  private static final UUID AARAV = UUID.fromString("d0000000-0000-0000-0000-000000000001");
  private static final UUID SANKRI = UUID.fromString("c0000000-0000-0000-0000-000000000002");
  private static final List<AlertStatus> ACTIVE =
      List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

  @Autowired private AlertRepository alertRepository;
  @Autowired private ParticipantRepository participantRepository;
  @Autowired private CheckpointRepository checkpointRepository;
  @Autowired private UserRepository userRepository;

  private Alert saveAlert() {
    return alertRepository.saveAndFlush(
        new Alert(
            participantRepository.findById(AARAV).orElseThrow(),
            checkpointRepository.findById(SANKRI).orElseThrow()));
  }

  @Test
  void idempotencyCheckSeesAnAlertInEveryStatus() {
    assertThat(alertRepository.existsByParticipantIdAndCheckpointId(AARAV, SANKRI)).isFalse();

    Alert alert = saveAlert();
    assertThat(alertRepository.existsByParticipantIdAndCheckpointId(AARAV, SANKRI)).isTrue();

    User ops = userRepository.findByUsername("ops").orElseThrow();
    alert.resolve(ops, Instant.parse("2026-08-10T13:00:00Z"));
    alertRepository.saveAndFlush(alert);
    // A resolved alert still counts, so the delay check does not raise it again.
    assertThat(alertRepository.existsByParticipantIdAndCheckpointId(AARAV, SANKRI)).isTrue();
  }

  @Test
  void activeAlertLookupIgnoresResolvedOnes() {
    Alert alert = saveAlert();
    assertThat(alertRepository.findByParticipantIdAndCheckpointIdAndStatusIn(AARAV, SANKRI, ACTIVE))
        .hasSize(1);

    alert.resolve(
        userRepository.findByUsername("ops").orElseThrow(), Instant.parse("2026-08-10T13:00:00Z"));
    alertRepository.saveAndFlush(alert);
    assertThat(alertRepository.findByParticipantIdAndCheckpointIdAndStatusIn(AARAV, SANKRI, ACTIVE))
        .isEmpty();
  }

  @Test
  void listQueriesFilterByStatusAndPage() {
    saveAlert();

    assertThat(
            alertRepository
                .findByStatus(AlertStatus.OPEN, PageRequest.of(0, 10))
                .getTotalElements())
        .isEqualTo(1);
    assertThat(
            alertRepository
                .findByStatus(AlertStatus.RESOLVED, PageRequest.of(0, 10))
                .getTotalElements())
        .isZero();
    assertThat(alertRepository.findAllBy(PageRequest.of(0, 10)).getContent())
        .extracting(a -> a.getParticipant().getName())
        .containsExactly("Aarav Sharma");
  }
}
