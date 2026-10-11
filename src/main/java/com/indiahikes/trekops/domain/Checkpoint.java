package com.indiahikes.trekops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "checkpoints",
    uniqueConstraints = @UniqueConstraint(columnNames = {"trek_id", "sequence_no"}))
public class Checkpoint extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trek_id")
  private Trek trek;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private int sequenceNo;

  /** Hours after the trek start time by which participants should have checked in. */
  @Column(nullable = false)
  private int expectedAfterHours;

  protected Checkpoint() {}

  public Checkpoint(Trek trek, String name, int sequenceNo, int expectedAfterHours) {
    this.trek = trek;
    this.name = name;
    this.sequenceNo = sequenceNo;
    this.expectedAfterHours = expectedAfterHours;
  }

  public Trek getTrek() {
    return trek;
  }

  public String getName() {
    return name;
  }

  public int getSequenceNo() {
    return sequenceNo;
  }

  public int getExpectedAfterHours() {
    return expectedAfterHours;
  }
}
