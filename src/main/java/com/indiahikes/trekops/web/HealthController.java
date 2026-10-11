package com.indiahikes.trekops.web;

import com.indiahikes.trekops.service.HealthService;
import com.indiahikes.trekops.web.dto.HealthResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public. Returns 200 when the database answers and 503 when it does not. */
@RestController
@Tag(name = "Health")
@SecurityRequirements
public class HealthController {

  private final HealthService healthService;

  public HealthController(HealthService healthService) {
    this.healthService = healthService;
  }

  @GetMapping("/api/health")
  public ResponseEntity<HealthResponse> health() {
    HealthResponse health = healthService.check();
    HttpStatus status =
        "UP".equals(health.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return ResponseEntity.status(status).body(health);
  }
}
