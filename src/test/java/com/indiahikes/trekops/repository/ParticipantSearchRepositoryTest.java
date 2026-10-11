package com.indiahikes.trekops.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.JpaAuditingConfig;
import com.indiahikes.trekops.config.SecurityAuditorAware;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the search filters as real SQL on H2 against the seed data: Aarav Sharma and Diya Mehta are
 * ON_TREK, Rohan Verma is REGISTERED, all on the Kedarkantha trek.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, ClockConfig.class, SecurityAuditorAware.class})
class ParticipantSearchRepositoryTest {

  private static final UUID SEED_TREK = UUID.fromString("b0000000-0000-0000-0000-000000000001");

  @Autowired private ParticipantRepository participantRepository;

  private List<String> namesFor(UUID trekId, ParticipantStatus status, String name, String phone) {
    return participantRepository
        .findAll(ParticipantSpecifications.search(trekId, status, name, phone), Sort.by("name"))
        .stream()
        .map(Participant::getName)
        .toList();
  }

  @Test
  void noFiltersReturnsEveryone() {
    assertThat(namesFor(null, null, null, null))
        .containsExactly("Aarav Sharma", "Diya Mehta", "Rohan Verma");
  }

  @Test
  void filtersByStatus() {
    assertThat(namesFor(null, ParticipantStatus.ON_TREK, null, null))
        .containsExactly("Aarav Sharma", "Diya Mehta");
  }

  @Test
  void nameMatchIsCaseInsensitiveAndPartial() {
    assertThat(namesFor(null, null, "DIYA", null)).containsExactly("Diya Mehta");
  }

  @Test
  void filtersCombineWithAnd() {
    assertThat(namesFor(SEED_TREK, ParticipantStatus.REGISTERED, "r", null))
        .containsExactly("Rohan Verma");
  }

  @Test
  void filtersByPartialPhone() {
    assertThat(namesFor(null, null, null, "00003")).containsExactly("Rohan Verma");
  }

  @Test
  void wildcardCharactersInInputMatchLiterally() {
    assertThat(namesFor(null, null, "%", null)).isEmpty();
    assertThat(namesFor(null, null, "_", null)).isEmpty();
  }

  @Test
  void unknownTrekMatchesNobody() {
    assertThat(namesFor(UUID.randomUUID(), null, null, null)).isEmpty();
  }

  @Test
  void pagesAndSorts() {
    Page<Participant> page =
        participantRepository.findAll(
            ParticipantSpecifications.search(null, null, null, null),
            PageRequest.of(1, 1, Sort.by(Sort.Direction.DESC, "name")));

    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getTotalPages()).isEqualTo(3);
    assertThat(page.getContent()).extracting(Participant::getName).containsExactly("Diya Mehta");
  }
}
