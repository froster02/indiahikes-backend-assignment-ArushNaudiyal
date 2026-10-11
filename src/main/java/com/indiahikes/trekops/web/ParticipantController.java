package com.indiahikes.trekops.web;

import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.service.ParticipantService;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.dto.ParticipantDetailResponse;
import com.indiahikes.trekops.web.dto.ParticipantResponse;
import com.indiahikes.trekops.web.dto.ParticipantStatusRequest;
import com.indiahikes.trekops.web.dto.RegisterParticipantRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Both roles may use every endpoint here. */
@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

  private final ParticipantService participantService;

  public ParticipantController(ParticipantService participantService) {
    this.participantService = participantService;
  }

  /**
   * Search. All filters are optional and combine with AND. Example: {@code
   * ?trekId=...&status=ON_TREK&name=sha&page=0&size=20&sort=name,asc}
   */
  @GetMapping
  public PageResponse<ParticipantResponse> search(
      @RequestParam(required = false) UUID trekId,
      @RequestParam(required = false) ParticipantStatus status,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String phone,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return participantService.search(trekId, status, name, phone, pageable);
  }

  @PostMapping
  public ResponseEntity<ParticipantResponse> register(
      @Valid @RequestBody RegisterParticipantRequest request) {
    ParticipantResponse participant =
        participantService.register(
            request.trekId(), request.name(), request.phone(), request.emergencyContact());
    return ResponseEntity.created(URI.create("/api/participants/" + participant.id()))
        .body(participant);
  }

  @GetMapping("/{id}")
  public ParticipantDetailResponse get(@PathVariable UUID id) {
    return participantService.get(id);
  }

  @PatchMapping("/{id}/status")
  public ParticipantResponse changeStatus(
      @PathVariable UUID id, @Valid @RequestBody ParticipantStatusRequest request) {
    return participantService.changeStatus(id, request.status());
  }
}
