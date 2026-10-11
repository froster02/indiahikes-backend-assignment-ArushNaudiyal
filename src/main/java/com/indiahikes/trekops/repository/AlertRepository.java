package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.AlertStatus;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

  /**
   * Idempotency guard. The scheduler creates an alert only when this returns false for the statuses
   * OPEN and ACKNOWLEDGED, so a pair never has two active alerts.
   */
  boolean existsByParticipantIdAndCheckpointIdAndStatusIn(
      UUID participantId, UUID checkpointId, Collection<AlertStatus> statuses);

  Page<Alert> findByStatus(AlertStatus status, Pageable pageable);
}
