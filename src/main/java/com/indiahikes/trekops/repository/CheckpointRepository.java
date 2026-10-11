package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.Checkpoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckpointRepository extends JpaRepository<Checkpoint, UUID> {

  List<Checkpoint> findByTrekIdOrderBySequenceNo(UUID trekId);

  boolean existsByTrekIdAndSequenceNo(UUID trekId, int sequenceNo);
}
