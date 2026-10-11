package com.indiahikes.trekops.service;

import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.CheckpointRepository;
import com.indiahikes.trekops.repository.TrekRepository;
import com.indiahikes.trekops.web.dto.CheckpointResponse;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.dto.TrekDetailResponse;
import com.indiahikes.trekops.web.dto.TrekResponse;
import com.indiahikes.trekops.web.mapper.TrekMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trek and checkpoint rules. Returns DTOs, so entities never leave this layer. */
@Service
public class TrekService {

  /** Allowed status changes. COMPLETED and CANCELLED are final. */
  private static final Map<TrekStatus, Set<TrekStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          TrekStatus.PLANNED, Set.of(TrekStatus.ACTIVE, TrekStatus.CANCELLED),
          TrekStatus.ACTIVE, Set.of(TrekStatus.COMPLETED, TrekStatus.CANCELLED),
          TrekStatus.COMPLETED, Set.of(),
          TrekStatus.CANCELLED, Set.of());

  private final TrekRepository trekRepository;
  private final CheckpointRepository checkpointRepository;

  public TrekService(TrekRepository trekRepository, CheckpointRepository checkpointRepository) {
    this.trekRepository = trekRepository;
    this.checkpointRepository = checkpointRepository;
  }

  @Transactional
  public TrekResponse create(String name, Instant startAt, Instant endAt) {
    Trek trek = trekRepository.save(new Trek(name, startAt, endAt));
    return TrekMapper.toResponse(trek);
  }

  @Transactional(readOnly = true)
  public PageResponse<TrekResponse> list(Pageable pageable) {
    return PageResponse.from(trekRepository.findAll(pageable).map(TrekMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public TrekDetailResponse get(UUID trekId) {
    Trek trek = findTrek(trekId);
    return TrekMapper.toDetail(trek, checkpointRepository.findByTrekIdOrderBySequenceNo(trekId));
  }

  @Transactional
  public TrekResponse changeStatus(UUID trekId, TrekStatus newStatus) {
    Trek trek = findTrek(trekId);
    if (!ALLOWED_TRANSITIONS.get(trek.getStatus()).contains(newStatus)) {
      throw new BusinessRuleException(
          "Trek status cannot change from " + trek.getStatus() + " to " + newStatus);
    }
    if (newStatus == TrekStatus.ACTIVE
        && checkpointRepository.findByTrekIdOrderBySequenceNo(trekId).isEmpty()) {
      throw new BusinessRuleException("A trek needs at least one checkpoint before it starts");
    }
    trek.setStatus(newStatus);
    return TrekMapper.toResponse(trekRepository.saveAndFlush(trek));
  }

  /**
   * Adds a checkpoint. Rules: the trek is not finished, the sequence number is free, the expected
   * time is inside the trek, and expected hours grow with the sequence number.
   */
  @Transactional
  public CheckpointResponse addCheckpoint(
      UUID trekId, String name, int sequenceNo, int expectedAfterHours) {
    Trek trek = findTrek(trekId);
    if (trek.getStatus() == TrekStatus.COMPLETED || trek.getStatus() == TrekStatus.CANCELLED) {
      throw new BusinessRuleException("Cannot add checkpoints to a " + trek.getStatus() + " trek");
    }
    if (checkpointRepository.existsByTrekIdAndSequenceNo(trekId, sequenceNo)) {
      throw new ConflictException("Sequence number " + sequenceNo + " is already used");
    }
    Instant expectedAt = trek.getStartAt().plus(Duration.ofHours(expectedAfterHours));
    if (expectedAt.isAfter(trek.getEndAt())) {
      throw new BusinessRuleException("Checkpoint is expected after the trek ends");
    }
    List<Checkpoint> existing = checkpointRepository.findByTrekIdOrderBySequenceNo(trekId);
    for (Checkpoint other : existing) {
      boolean earlierButLater =
          other.getSequenceNo() < sequenceNo && other.getExpectedAfterHours() > expectedAfterHours;
      boolean laterButEarlier =
          other.getSequenceNo() > sequenceNo && other.getExpectedAfterHours() < expectedAfterHours;
      if (earlierButLater || laterButEarlier) {
        throw new BusinessRuleException("Expected hours must follow the checkpoint order");
      }
    }
    Checkpoint checkpoint =
        checkpointRepository.save(new Checkpoint(trek, name, sequenceNo, expectedAfterHours));
    return TrekMapper.toResponse(checkpoint);
  }

  private Trek findTrek(UUID trekId) {
    return trekRepository
        .findById(trekId)
        .orElseThrow(() -> new ResourceNotFoundException("Trek " + trekId + " not found"));
  }
}
