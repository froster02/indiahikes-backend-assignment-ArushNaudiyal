package com.indiahikes.trekops.service;

import com.indiahikes.trekops.domain.Alert;
import com.indiahikes.trekops.domain.AlertStatus;
import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.repository.AlertRepository;
import com.indiahikes.trekops.repository.UserRepository;
import com.indiahikes.trekops.web.dto.AlertResponse;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.mapper.AlertMapper;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Viewing and managing alerts. Allowed changes: OPEN to ACKNOWLEDGED, OPEN to RESOLVED and
 * ACKNOWLEDGED to RESOLVED. RESOLVED is final.
 */
@Service
public class AlertService {

  private final AlertRepository alertRepository;
  private final UserRepository userRepository;
  private final Clock clock;

  public AlertService(AlertRepository alertRepository, UserRepository userRepository, Clock clock) {
    this.alertRepository = alertRepository;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<AlertResponse> list(AlertStatus status, Pageable pageable) {
    Page<Alert> page =
        status == null
            ? alertRepository.findAllBy(pageable)
            : alertRepository.findByStatus(status, pageable);
    return PageResponse.from(page.map(AlertMapper::toResponse));
  }

  @Transactional
  public AlertResponse acknowledge(UUID alertId, String username) {
    Alert alert = findAlert(alertId);
    if (alert.getStatus() != AlertStatus.OPEN) {
      throw new BusinessRuleException(
          "Only an OPEN alert can be acknowledged; this one is " + alert.getStatus());
    }
    alert.acknowledge(findUser(username), clock.instant());
    return AlertMapper.toResponse(alertRepository.saveAndFlush(alert));
  }

  @Transactional
  public AlertResponse resolve(UUID alertId, String username) {
    Alert alert = findAlert(alertId);
    if (alert.getStatus() == AlertStatus.RESOLVED) {
      throw new BusinessRuleException("Alert is already RESOLVED");
    }
    alert.resolve(findUser(username), clock.instant());
    return AlertMapper.toResponse(alertRepository.saveAndFlush(alert));
  }

  private Alert findAlert(UUID alertId) {
    return alertRepository
        .findById(alertId)
        .orElseThrow(() -> new ResourceNotFoundException("Alert " + alertId + " not found"));
  }

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new BadCredentialsException("Unknown user"));
  }
}
