package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.TrekRepository;
import com.indiahikes.trekops.web.dto.CheckpointResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrekServiceTest {

  private static final Instant START = Instant.parse("2026-08-10T00:30:00Z");
  // 84 hours after the start, like the Kedarkantha example.
  private static final Instant END = Instant.parse("2026-08-13T12:30:00Z");

  private final TrekRepository trekRepository = mock(TrekRepository.class);
  private final CheckpointRepository checkpointRepository = mock(CheckpointRepository.class);
  private final TrekService service = new TrekService(trekRepository, checkpointRepository);

  private final UUID trekId = UUID.randomUUID();
  private Trek trek;

  @BeforeEach
  void setUp() {
    trek = new Trek("Kedarkantha Trek", START, END);
    when(trekRepository.findById(trekId)).thenReturn(Optional.of(trek));
    when(trekRepository.save(any(Trek.class))).thenAnswer(call -> call.getArgument(0));
    when(trekRepository.saveAndFlush(any(Trek.class))).thenAnswer(call -> call.getArgument(0));
    when(checkpointRepository.save(any(Checkpoint.class))).thenAnswer(call -> call.getArgument(0));
  }

  private void givenCheckpoints(Checkpoint... checkpoints) {
    when(checkpointRepository.findByTrekIdOrderBySequenceNo(trekId))
        .thenReturn(List.of(checkpoints));
  }

  @Test
  void aNewTrekStartsAsPlanned() {
    assertThat(service.create("New Trek", START, END).status()).isEqualTo(TrekStatus.PLANNED);
  }

  @Test
  void detailListsCheckpointsInSequenceOrder() {
    givenCheckpoints(new Checkpoint(trek, "Dehradun", 1, 0), new Checkpoint(trek, "Sankri", 2, 8));

    assertThat(service.get(trekId).checkpoints())
        .extracting(CheckpointResponse::name)
        .containsExactly("Dehradun", "Sankri");
  }

  @Test
  void unknownTrekIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(trekRepository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get(unknown)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void plannedTrekWithCheckpointsCanStart() {
    givenCheckpoints(new Checkpoint(trek, "Dehradun", 1, 0));

    assertThat(service.changeStatus(trekId, TrekStatus.ACTIVE).status())
        .isEqualTo(TrekStatus.ACTIVE);
  }

  @Test
  void trekWithoutCheckpointsCannotStart() {
    givenCheckpoints();

    assertThatThrownBy(() -> service.changeStatus(trekId, TrekStatus.ACTIVE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void finishedTrekCannotChangeStatus() {
    trek.setStatus(TrekStatus.COMPLETED);

    assertThatThrownBy(() -> service.changeStatus(trekId, TrekStatus.ACTIVE))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("COMPLETED to ACTIVE");
  }

  @Test
  void plannedTrekCannotJumpToCompleted() {
    assertThatThrownBy(() -> service.changeStatus(trekId, TrekStatus.COMPLETED))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void validCheckpointIsSaved() {
    givenCheckpoints(new Checkpoint(trek, "Dehradun", 1, 0));

    assertThat(service.addCheckpoint(trekId, "Sankri", 2, 8).name()).isEqualTo("Sankri");
    verify(checkpointRepository).save(any(Checkpoint.class));
  }

  @Test
  void usedSequenceNumberIsAConflict() {
    when(checkpointRepository.existsByTrekIdAndSequenceNo(trekId, 1)).thenReturn(true);

    assertThatThrownBy(() -> service.addCheckpoint(trekId, "Dehradun", 1, 0))
        .isInstanceOf(ConflictException.class);
    verify(checkpointRepository, never()).save(any());
  }

  @Test
  void checkpointAfterTheTrekEndsIsRejected() {
    givenCheckpoints();

    assertThatThrownBy(() -> service.addCheckpoint(trekId, "Too late", 1, 85))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void expectedHoursMustFollowTheSequence() {
    givenCheckpoints(new Checkpoint(trek, "Juda Ka Talab", 3, 28));

    assertThatThrownBy(() -> service.addCheckpoint(trekId, "Sankri", 2, 30))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Expected hours must follow the checkpoint order");
  }

  @Test
  void cancelledTrekTakesNoNewCheckpoints() {
    trek.setStatus(TrekStatus.CANCELLED);

    assertThatThrownBy(() -> service.addCheckpoint(trekId, "Dehradun", 1, 0))
        .isInstanceOf(BusinessRuleException.class);
  }
}
