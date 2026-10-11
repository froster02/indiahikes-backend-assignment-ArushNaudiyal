package com.indiahikes.trekops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/** One report by a participant at a checkpoint. A participant checks in once per checkpoint. */
@Entity
@Table(
    name = "check_ins",
    uniqueConstraints = @UniqueConstraint(columnNames = {"participant_id", "checkpoint_id"}))
public class CheckIn extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participant_id")
  private Participant participant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "checkpoint_id")
  private Checkpoint checkpoint;

  @Column(nullable = false)
  private Instant checkedInAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "recorded_by")
  private User recordedBy;

  protected CheckIn() {}

  public CheckIn(
      Participant participant, Checkpoint checkpoint, Instant checkedInAt, User recordedBy) {
    this.participant = participant;
    this.checkpoint = checkpoint;
    this.checkedInAt = checkedInAt;
    this.recordedBy = recordedBy;
  }

  public Participant getParticipant() {
    return participant;
  }

  public Checkpoint getCheckpoint() {
    return checkpoint;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public User getRecordedBy() {
    return recordedBy;
  }
}
