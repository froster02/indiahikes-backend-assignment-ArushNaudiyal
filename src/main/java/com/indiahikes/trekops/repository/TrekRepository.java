package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.TrekStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrekRepository extends JpaRepository<Trek, UUID> {

  /** Used by the delay scheduler to scan only ACTIVE treks. */
  List<Trek> findByStatus(TrekStatus status);
}
