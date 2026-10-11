package com.indiahikes.trekops.service;

import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.InvalidRequestException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.CheckInRepository;
import com.indiahikes.trekops.repository.ParticipantRepository;
import com.indiahikes.trekops.repository.ParticipantSpecifications;
import com.indiahikes.trekops.repository.TrekRepository;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.dto.ParticipantDetailResponse;
import com.indiahikes.trekops.web.dto.ParticipantResponse;
import com.indiahikes.trekops.web.mapper.ParticipantMapper;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Participant rules and search. Returns DTOs, so entities never leave this layer. */
@Service
public class ParticipantService {

  /** Sorting is limited to these fields, so clients cannot sort on internal columns. */
  static final Set<String> SORTABLE_FIELDS = Set.of("name", "status", "createdAt", "updatedAt");

  /** Allowed status changes. COMPLETED and DROPPED are final. */
  private static final Map<ParticipantStatus, Set<ParticipantStatus>> ALLOWED_TRANSITIONS =
      Map.of(
          ParticipantStatus.REGISTERED,
              Set.of(ParticipantStatus.ON_TREK, ParticipantStatus.DROPPED),
          ParticipantStatus.ON_TREK, Set.of(ParticipantStatus.COMPLETED, ParticipantStatus.DROPPED),
          ParticipantStatus.COMPLETED, Set.of(),
          ParticipantStatus.DROPPED, Set.of());

  private final ParticipantRepository participantRepository;
  private final TrekRepository trekRepository;
  private final CheckInRepository checkInRepository;

  public ParticipantService(
      ParticipantRepository participantRepository,
      TrekRepository trekRepository,
      CheckInRepository checkInRepository) {
    this.participantRepository = participantRepository;
    this.trekRepository = trekRepository;
    this.checkInRepository = checkInRepository;
  }

  @Transactional
  public ParticipantResponse register(
      UUID trekId, String name, String phone, String emergencyContact) {
    Trek trek =
        trekRepository
            .findById(trekId)
            .orElseThrow(() -> new ResourceNotFoundException("Trek " + trekId + " not found"));
    if (trek.getStatus() == TrekStatus.COMPLETED || trek.getStatus() == TrekStatus.CANCELLED) {
      throw new BusinessRuleException("Cannot register for a " + trek.getStatus() + " trek");
    }
    Participant participant =
        participantRepository.save(new Participant(trek, name, phone, emergencyContact));
    return ParticipantMapper.toResponse(participant);
  }

  @Transactional(readOnly = true)
  public ParticipantDetailResponse get(UUID participantId) {
    Participant participant = findParticipant(participantId);
    return ParticipantMapper.toDetail(
        participant, checkInRepository.findByParticipantIdOrderByCheckedInAt(participantId));
  }

  /**
   * Moves a participant along REGISTERED, ON_TREK, COMPLETED or DROPPED. Going ON_TREK needs an
   * ACTIVE trek, because the delay scheduler only watches ON_TREK participants of ACTIVE treks.
   */
  @Transactional
  public ParticipantResponse changeStatus(UUID participantId, ParticipantStatus newStatus) {
    Participant participant = findParticipant(participantId);
    if (!ALLOWED_TRANSITIONS.get(participant.getStatus()).contains(newStatus)) {
      throw new BusinessRuleException(
          "Participant status cannot change from " + participant.getStatus() + " to " + newStatus);
    }
    if (newStatus == ParticipantStatus.ON_TREK
        && participant.getTrek().getStatus() != TrekStatus.ACTIVE) {
      throw new BusinessRuleException("A participant can go on trek only when the trek is ACTIVE");
    }
    participant.setStatus(newStatus);
    return ParticipantMapper.toResponse(participantRepository.saveAndFlush(participant));
  }

  @Transactional(readOnly = true)
  public PageResponse<ParticipantResponse> search(
      UUID trekId, ParticipantStatus status, String name, String phone, Pageable pageable) {
    for (Sort.Order order : pageable.getSort()) {
      if (!SORTABLE_FIELDS.contains(order.getProperty())) {
        throw new InvalidRequestException(
            "Cannot sort by "
                + order.getProperty()
                + ". Allowed: "
                + new TreeSet<>(SORTABLE_FIELDS));
      }
    }
    var spec = ParticipantSpecifications.search(trekId, status, name, phone);
    return PageResponse.from(
        participantRepository.findAll(spec, pageable).map(ParticipantMapper::toResponse));
  }

  private Participant findParticipant(UUID participantId) {
    return participantRepository
        .findById(participantId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Participant " + participantId + " not found"));
  }
}
