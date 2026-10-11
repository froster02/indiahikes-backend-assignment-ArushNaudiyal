package com.indiahikes.trekops.web;

import com.indiahikes.trekops.service.TrekService;
import com.indiahikes.trekops.web.dto.CheckpointResponse;
import com.indiahikes.trekops.web.dto.CreateCheckpointRequest;
import com.indiahikes.trekops.web.dto.CreateTrekRequest;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.dto.TrekDetailResponse;
import com.indiahikes.trekops.web.dto.TrekResponse;
import com.indiahikes.trekops.web.dto.TrekStatusRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

/** Role rules for these paths live in SecurityConfig. */
@RestController
@Tag(name = "Treks")
@RequestMapping("/api/treks")
public class TrekController {

  private final TrekService trekService;

  public TrekController(TrekService trekService) {
    this.trekService = trekService;
  }

  @GetMapping
  public PageResponse<TrekResponse> list(
      @PageableDefault(size = 20, sort = "startAt") Pageable pageable) {
    return trekService.list(pageable);
  }

  @PostMapping
  public ResponseEntity<TrekResponse> create(@Valid @RequestBody CreateTrekRequest request) {
    TrekResponse trek = trekService.create(request.name(), request.startAt(), request.endAt());
    return ResponseEntity.created(URI.create("/api/treks/" + trek.id())).body(trek);
  }

  @GetMapping("/{id}")
  public TrekDetailResponse get(@PathVariable UUID id) {
    return trekService.get(id);
  }

  @PatchMapping("/{id}/status")
  public TrekResponse changeStatus(
      @PathVariable UUID id, @Valid @RequestBody TrekStatusRequest request) {
    return trekService.changeStatus(id, request.status());
  }

  @PostMapping("/{id}/checkpoints")
  public ResponseEntity<CheckpointResponse> addCheckpoint(
      @PathVariable UUID id, @Valid @RequestBody CreateCheckpointRequest request) {
    CheckpointResponse checkpoint =
        trekService.addCheckpoint(
            id, request.name(), request.sequenceNo(), request.expectedAfterHours());
    return ResponseEntity.created(URI.create("/api/treks/" + id)).body(checkpoint);
  }
}
