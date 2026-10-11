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
import com.indiahikes.trekops.exception.InvalidRequestException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.TrekRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class ParticipantServiceTest {

  private final ParticipantRepository participantRepository = mock(ParticipantRepository.class);
  private final TrekRepository trekRepository = mock(TrekRepository.class);
  private final CheckInRepository checkInRepository = mock(CheckInRepository.class);
  private final ParticipantService service =
      new ParticipantService(participantRepository, trekRepository, checkInRepository);

  private final UUID trekId = UUID.randomUUID();
  private final UUID participantId = UUID.randomUUID();
  private Trek trek;
  private Participant participant;

  @BeforeEach
  void setUp() {
    trek =
        new Trek(
            "Kedarkantha Trek",
            Instant.parse("2026-08-10T00:30:00Z"),
            Instant.parse("2026-08-13T12:30:00Z"));
    participant = new Participant(trek, "Aarav Sharma", "+91 90000 00001", "+91 90000 10001");
    when(trekRepository.findById(trekId)).thenReturn(Optional.of(trek));
    when(participantRepository.findById(participantId)).thenReturn(Optional.of(participant));
    when(participantRepository.save(any(Participant.class)))
        .thenAnswer(call -> call.getArgument(0));
    when(participantRepository.saveAndFlush(any(Participant.class)))
        .thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void registeringStartsAsRegistered() {
    assertThat(service.register(trekId, "Diya Mehta", "+91 90000 00002", "Mother").status())
        .isEqualTo(ParticipantStatus.REGISTERED);
  }

  @Test
  void detailIncludesTheCheckInsInTimeOrder() {
    Checkpoint dehradun = new Checkpoint(trek, "Dehradun", 1, 0);
    User ops = new User("ops", "hash", UserRole.OPERATIONS);
    when(checkInRepository.findByParticipantIdOrderByCheckedInAt(participantId))
        .thenReturn(
            List.of(
                new CheckIn(participant, dehradun, Instant.parse("2026-08-10T00:20:00Z"), ops)));

    var detail = service.get(participantId);

    assertThat(detail.participant().name()).isEqualTo("Aarav Sharma");
    assertThat(detail.checkIns()).extracting(c -> c.checkpointName()).containsExactly("Dehradun");
    assertThat(detail.checkIns().get(0).recordedBy()).isEqualTo("ops");
  }

  @Test
  void unknownParticipantIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(participantRepository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void registeringForAnUnknownTrekIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(trekRepository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.register(unknown, "X", "+91 90000 00009", "Y"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void registeringForAFinishedTrekIsRejected() {
    trek.setStatus(TrekStatus.COMPLETED);

    assertThatThrownBy(() -> service.register(trekId, "X", "+91 90000 00009", "Y"))
        .isInstanceOf(BusinessRuleException.class);
    verify(participantRepository, never()).save(any());
  }

  @Test
  void participantGoesOnTrekWhenTheTrekIsActive() {
    trek.setStatus(TrekStatus.ACTIVE);

    assertThat(service.changeStatus(participantId, ParticipantStatus.ON_TREK).status())
        .isEqualTo(ParticipantStatus.ON_TREK);
  }

  @Test
  void participantCannotGoOnTrekBeforeTheTrekStarts() {
    assertThatThrownBy(() -> service.changeStatus(participantId, ParticipantStatus.ON_TREK))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("ACTIVE");
  }

  @Test
  void registeredParticipantCannotJumpToCompleted() {
    assertThatThrownBy(() -> service.changeStatus(participantId, ParticipantStatus.COMPLETED))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("REGISTERED to COMPLETED");
  }

  @Test
  void droppedIsFinal() {
    participant.setStatus(ParticipantStatus.DROPPED);

    assertThatThrownBy(() -> service.changeStatus(participantId, ParticipantStatus.ON_TREK))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void sortingOnAFieldOutsideTheWhitelistIsRejected() {
    Pageable byPhone = PageRequest.of(0, 20, Sort.by("phone"));

    assertThatThrownBy(() -> service.search(null, null, null, null, byPhone))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("phone");
  }

  @Test
  @SuppressWarnings("unchecked")
  void sortingOnAWhitelistedFieldRunsTheSearch() {
    Pageable byName = PageRequest.of(0, 20, Sort.by("name"));
    when(participantRepository.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty(byName));

    assertThat(service.search(null, ParticipantStatus.ON_TREK, "sha", null, byName).totalElements())
        .isZero();
  }
}
