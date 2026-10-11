package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** JpaSpecificationExecutor lets the search API (step 3.3) compose filters dynamically. */
public interface ParticipantRepository
    extends JpaRepository<Participant, UUID>, JpaSpecificationExecutor<Participant> {

  /** Used by the delay scheduler to scan only ON_TREK participants of a trek. */
  List<Participant> findByTrekIdAndStatus(UUID trekId, ParticipantStatus status);
}
