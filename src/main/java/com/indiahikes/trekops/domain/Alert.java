package com.indiahikes.trekops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Raised when a participant has not checked in at a checkpoint by its expected time. */
@Entity
@Table(name = "alerts")
public class Alert extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "participant_id")
  private Participant participant;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "checkpoint_id")
  private Checkpoint checkpoint;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AlertStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "acknowledged_by")
  private User acknowledgedBy;

  private Instant acknowledgedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resolved_by")
  private User resolvedBy;

  private Instant resolvedAt;

  protected Alert() {}

  public Alert(Participant participant, Checkpoint checkpoint) {
    this.participant = participant;
    this.checkpoint = checkpoint;
    this.status = AlertStatus.OPEN;
  }

  public Participant getParticipant() {
    return participant;
  }

  public Checkpoint getCheckpoint() {
    return checkpoint;
  }

  public AlertStatus getStatus() {
    return status;
  }

  public User getAcknowledgedBy() {
    return acknowledgedBy;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public User getResolvedBy() {
    return resolvedBy;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  /** Moves OPEN to ACKNOWLEDGED. The caller checks that the transition is allowed. */
  public void acknowledge(User user, Instant at) {
    this.status = AlertStatus.ACKNOWLEDGED;
    this.acknowledgedBy = user;
    this.acknowledgedAt = at;
  }

  /** Moves OPEN or ACKNOWLEDGED to RESOLVED. The caller checks that the transition is allowed. */
  public void resolve(User user, Instant at) {
    this.status = AlertStatus.RESOLVED;
    this.resolvedBy = user;
    this.resolvedAt = at;
  }
}
