package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.AlertStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

  /**
   * Idempotency guard. The delay check creates an alert only when this returns false, so a pair
   * gets at most one alert, whatever its status.
   */
  boolean existsByParticipantIdAndCheckpointId(UUID participantId, UUID checkpointId);

  /** Active alerts for one pair. A check-in uses this to resolve them. */
  List<Alert> findByParticipantIdAndCheckpointIdAndStatusIn(
      UUID participantId, UUID checkpointId, Collection<AlertStatus> statuses);

  // The entity graph loads the related rows in the same query, so a page of alerts does not
  // trigger one extra query per alert.
  @EntityGraph(
      attributePaths = {
        "participant",
        "participant.trek",
        "checkpoint",
        "acknowledgedBy",
        "resolvedBy"
      })
  Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

  @EntityGraph(
      attributePaths = {
        "participant",
        "participant.trek",
        "checkpoint",
        "acknowledgedBy",
        "resolvedBy"
      })
  Page<Alert> findAllBy(Pageable pageable);
}
