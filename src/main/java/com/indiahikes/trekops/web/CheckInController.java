package com.indiahikes.trekops.web;

import com.indiahikes.trekops.service.CheckInService;
import com.indiahikes.trekops.web.dto.CheckInResponse;
import com.indiahikes.trekops.web.dto.RecordCheckInRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Both roles may record check-ins. The recorder is always the logged in user. */
@RestController
@RequestMapping("/api/check-ins")
public class CheckInController {

  private final CheckInService checkInService;

  public CheckInController(CheckInService checkInService) {
    this.checkInService = checkInService;
  }

  @PostMapping
  public ResponseEntity<CheckInResponse> record(
      @Valid @RequestBody RecordCheckInRequest request, Authentication authentication) {
    CheckInResponse checkIn =
        checkInService.record(
            request.participantId(),
            request.checkpointId(),
            request.checkedInAt(),
            authentication.getName());
    return ResponseEntity.created(URI.create("/api/participants/" + checkIn.participantId()))
        .body(checkIn);
  }
}
