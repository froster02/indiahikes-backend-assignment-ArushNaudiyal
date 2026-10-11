package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.AlertStatus;
import com.indiahikes.trekops.domain.Checkpoint;
import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.Trek;
import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.domain.UserRole;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.AlertRepository;
import com.indiahikes.trekops.repository.UserRepository;
import com.indiahikes.trekops.web.dto.AlertResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AlertServiceTest {

  private static final Instant START = Instant.parse("2026-08-10T00:30:00Z");
  private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");

  private final AlertRepository alertRepository = mock(AlertRepository.class);
  private final UserRepository userRepository = mock(UserRepository.class);
  private final AlertService service =
      new AlertService(alertRepository, userRepository, Clock.fixed(NOW, ZoneOffset.UTC));

  private final UUID alertId = UUID.randomUUID();
  private Alert alert;

  @BeforeEach
  void setUp() {
    Trek trek = new Trek("Kedarkantha Trek", START, Instant.parse("2026-08-13T12:30:00Z"));
    Participant participant = new Participant(trek, "Aarav Sharma", "+91 90000 00001", "x");
    Checkpoint sankri = new Checkpoint(trek, "Sankri", 2, 8);
    alert = new Alert(participant, sankri);
    when(alertRepository.findById(alertId)).thenReturn(Optional.of(alert));
    when(alertRepository.saveAndFlush(any(Alert.class))).thenAnswer(c -> c.getArgument(0));
    when(userRepository.findByUsername("ops"))
        .thenReturn(Optional.of(new User("ops", "hash", UserRole.OPERATIONS)));
  }

  @Test
  void acknowledgingAnOpenAlertRecordsWhoAndWhen() {
    AlertResponse response = service.acknowledge(alertId, "ops");

    assertThat(response.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(response.acknowledgedBy()).isEqualTo("ops");
    assertThat(response.acknowledgedAt()).isEqualTo(NOW);
  }

  @Test
  void responseShowsWhenTheParticipantWasExpected() {
    // Sankri is expected 8 hours after the trek start.
    assertThat(service.acknowledge(alertId, "ops").expectedAt())
        .isEqualTo(Instant.parse("2026-08-10T08:30:00Z"));
  }

  @Test
  void openAlertCanBeResolvedDirectly() {
    AlertResponse response = service.resolve(alertId, "ops");

    assertThat(response.status()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(response.resolvedBy()).isEqualTo("ops");
    assertThat(response.resolvedAt()).isEqualTo(NOW);
  }

  @Test
  void acknowledgedAlertCanBeResolved() {
    alert.acknowledge(new User("admin", "hash", UserRole.ADMIN), START);

    assertThat(service.resolve(alertId, "ops").status()).isEqualTo(AlertStatus.RESOLVED);
  }

  @Test
  void acknowledgedAlertCannotBeAcknowledgedAgain() {
    alert.acknowledge(new User("admin", "hash", UserRole.ADMIN), START);

    assertThatThrownBy(() -> service.acknowledge(alertId, "ops"))
        .isInstanceOf(BusinessRuleException.class);
    verify(alertRepository, never()).saveAndFlush(any());
  }

  @Test
  void resolvedIsFinal() {
    alert.resolve(new User("admin", "hash", UserRole.ADMIN), START);

    assertThatThrownBy(() -> service.resolve(alertId, "ops"))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> service.acknowledge(alertId, "ops"))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void listWithoutStatusReturnsEveryAlert() {
    Pageable page = PageRequest.of(0, 20);
    when(alertRepository.findAllBy(page)).thenReturn(new PageImpl<>(List.of(alert), page, 1));

    assertThat(service.list(null, page).content())
        .extracting(AlertResponse::checkpointName)
        .containsExactly("Sankri");
    verify(alertRepository, never()).findByStatus(any(), any());
  }

  @Test
  void listWithStatusFilters() {
    Pageable page = PageRequest.of(0, 20);
    when(alertRepository.findByStatus(AlertStatus.OPEN, page))
        .thenReturn(new PageImpl<>(List.of(alert), page, 1));

    assertThat(service.list(AlertStatus.OPEN, page).totalElements()).isEqualTo(1);
    verify(alertRepository, never()).findAllBy(any());
  }

  @Test
  void unknownAlertIsNotFound() {
    UUID unknown = UUID.randomUUID();
    when(alertRepository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.acknowledge(unknown, "ops"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
