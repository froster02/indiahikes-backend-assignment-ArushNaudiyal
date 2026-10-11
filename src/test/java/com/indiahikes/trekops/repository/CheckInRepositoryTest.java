package com.indiahikes.trekops.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.JpaAuditingConfig;
import com.indiahikes.trekops.config.SecurityAuditorAware;
import com.indiahikes.trekops.domain.CheckIn;
import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/** Uses the seed data on H2. Proves the database itself stops a second check-in. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, ClockConfig.class, SecurityAuditorAware.class})
class CheckInRepositoryTest {

  private static final UUID AARAV = UUID.fromString("d0000000-0000-0000-0000-000000000001");
  private static final UUID DEHRADUN = UUID.fromString("c0000000-0000-0000-0000-000000000001");
  private static final UUID SANKRI = UUID.fromString("c0000000-0000-0000-0000-000000000002");

  @Autowired private CheckInRepository checkInRepository;
  @Autowired private ParticipantRepository participantRepository;
  @Autowired private CheckpointRepository checkpointRepository;
  @Autowired private UserRepository userRepository;

  private CheckIn checkIn(UUID checkpointId) {
    Participant participant = participantRepository.findById(AARAV).orElseThrow();
    Checkpoint checkpoint = checkpointRepository.findById(checkpointId).orElseThrow();
    User admin = userRepository.findByUsername("admin").orElseThrow();
    return new CheckIn(participant, checkpoint, Instant.parse("2026-08-10T01:00:00Z"), admin);
  }

  @Test
  void existsCheckSeesOnlyTheSavedPair() {
    assertThat(checkInRepository.existsByParticipantIdAndCheckpointId(AARAV, DEHRADUN)).isFalse();

    checkInRepository.saveAndFlush(checkIn(DEHRADUN));

    assertThat(checkInRepository.existsByParticipantIdAndCheckpointId(AARAV, DEHRADUN)).isTrue();
    assertThat(checkInRepository.existsByParticipantIdAndCheckpointId(AARAV, SANKRI)).isFalse();
  }

  @Test
  void databaseRejectsASecondCheckInForTheSamePair() {
    checkInRepository.saveAndFlush(checkIn(DEHRADUN));

    assertThatThrownBy(() -> checkInRepository.saveAndFlush(checkIn(DEHRADUN)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void listsCheckInsInTimeOrder() {
    CheckIn later = checkIn(SANKRI);
    CheckIn earlier = checkIn(DEHRADUN);
    checkInRepository.saveAndFlush(later);
    checkInRepository.saveAndFlush(earlier);

    assertThat(checkInRepository.findByParticipantIdOrderByCheckedInAt(AARAV)).hasSize(2);
  }
}
