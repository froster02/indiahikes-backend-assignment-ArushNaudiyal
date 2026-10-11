package com.indiahikes.trekops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "treks")
public class Trek extends BaseEntity {

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Instant startAt;

  @Column(nullable = false)
  private Instant endAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TrekStatus status;

  protected Trek() {}

  public Trek(String name, Instant startAt, Instant endAt) {
    this.name = name;
    this.startAt = startAt;
    this.endAt = endAt;
    this.status = TrekStatus.PLANNED;
  }

  public String getName() {
    return name;
  }

  public Instant getStartAt() {
    return startAt;
  }

  public Instant getEndAt() {
    return endAt;
  }

  public TrekStatus getStatus() {
    return status;
  }

  public void setStatus(TrekStatus status) {
    this.status = status;
  }
}
