package com.indiahikes.trekops.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "participants")
public class Participant extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "trek_id")
  private Trek trek;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false)
  private String emergencyContact;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ParticipantStatus status;

  protected Participant() {}

  public Participant(Trek trek, String name, String phone, String emergencyContact) {
    this.trek = trek;
    this.name = name;
    this.phone = phone;
    this.emergencyContact = emergencyContact;
    this.status = ParticipantStatus.REGISTERED;
  }

  public Trek getTrek() {
    return trek;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmergencyContact() {
    return emergencyContact;
  }

  public ParticipantStatus getStatus() {
    return status;
  }

  public void setStatus(ParticipantStatus status) {
    this.status = status;
  }
}
