package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.CheckIn;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {

  /** True when the participant already reported at the checkpoint. Backs the delay rule. */
  boolean existsByParticipantIdAndCheckpointId(UUID participantId, UUID checkpointId);

  List<CheckIn> findByParticipantIdOrderByCheckedInAt(UUID participantId);
}
