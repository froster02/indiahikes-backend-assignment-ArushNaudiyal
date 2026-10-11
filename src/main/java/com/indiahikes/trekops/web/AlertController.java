package com.indiahikes.trekops.web;

import com.indiahikes.trekops.domain.AlertStatus;
import com.indiahikes.trekops.service.AlertService;
import com.indiahikes.trekops.web.dto.AlertResponse;
import com.indiahikes.trekops.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Both roles may view and manage alerts. Newest alerts come first by default. */
@RestController
@Tag(name = "Alerts")
@RequestMapping("/api/alerts")
public class AlertController {

  private final AlertService alertService;

  public AlertController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping
  public PageResponse<AlertResponse> list(
      @RequestParam(required = false) AlertStatus status,
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return alertService.list(status, pageable);
  }

  @PostMapping("/{id}/acknowledge")
  public AlertResponse acknowledge(@PathVariable UUID id, Authentication authentication) {
    return alertService.acknowledge(id, authentication.getName());
  }

  @PostMapping("/{id}/resolve")
  public AlertResponse resolve(@PathVariable UUID id, Authentication authentication) {
    return alertService.resolve(id, authentication.getName());
  }
}
