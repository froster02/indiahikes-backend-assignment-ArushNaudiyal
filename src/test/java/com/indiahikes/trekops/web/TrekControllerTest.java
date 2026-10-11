package com.indiahikes.trekops.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.SecurityConfig;
import com.indiahikes.trekops.config.SecurityErrorHandler;
import com.indiahikes.trekops.domain.TrekStatus;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.service.JwtService;
import com.indiahikes.trekops.service.TrekService;
import com.indiahikes.trekops.web.dto.CheckpointResponse;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.dto.TrekDetailResponse;
import com.indiahikes.trekops.web.dto.TrekResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TrekController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class TrekControllerTest {

  private static final UUID TREK_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
  private static final Instant START = Instant.parse("2026-08-10T00:30:00Z");
  private static final Instant END = Instant.parse("2026-08-13T12:30:00Z");

  @Autowired private MockMvc mockMvc;
  @MockBean private TrekService trekService;
  @MockBean private JwtService jwtService;

  private static RequestPostProcessor admin() {
    return user("admin").roles("ADMIN");
  }

  private static RequestPostProcessor ops() {
    return user("ops").roles("OPERATIONS");
  }

  private static TrekResponse trek(TrekStatus status) {
    return new TrekResponse(
        TREK_ID, "Kedarkantha Trek", START, END, status, START, "admin", START, "admin");
  }

  private static final String VALID_TREK =
      "{\"name\":\"Kedarkantha Trek\",\"startAt\":\"2026-08-10T00:30:00Z\","
          + "\"endAt\":\"2026-08-13T12:30:00Z\"}";

  @Test
  void adminCreatesATrekAndGets201WithLocation() throws Exception {
    when(trekService.create("Kedarkantha Trek", START, END)).thenReturn(trek(TrekStatus.PLANNED));

    mockMvc
        .perform(
            post("/api/treks")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_TREK))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/treks/" + TREK_ID))
        .andExpect(jsonPath("$.status").value("PLANNED"))
        .andExpect(jsonPath("$.createdBy").value("admin"));
  }

  @Test
  void endBeforeStartIs400() throws Exception {
    String body =
        "{\"name\":\"Bad\",\"startAt\":\"2026-08-13T00:00:00Z\",\"endAt\":\"2026-08-10T00:00:00Z\"}";

    mockMvc
        .perform(
            post("/api/treks").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.endAfterStart").value("endAt must be after startAt"));
  }

  @Test
  void missingNameIs400() throws Exception {
    String body = "{\"startAt\":\"2026-08-10T00:00:00Z\",\"endAt\":\"2026-08-13T00:00:00Z\"}";

    mockMvc
        .perform(
            post("/api/treks").with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists());
  }

  @Test
  void operationsCannotCreateATrek() throws Exception {
    mockMvc
        .perform(
            post("/api/treks")
                .with(ops())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_TREK))
        .andExpect(status().isForbidden());
  }

  @Test
  void operationsCanListTreks() throws Exception {
    when(trekService.list(any()))
        .thenReturn(new PageResponse<>(List.of(trek(TrekStatus.ACTIVE)), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/treks").with(ops()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Kedarkantha Trek"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void detailReturnsTheTrekAndItsCheckpoints() throws Exception {
    when(trekService.get(TREK_ID))
        .thenReturn(
            new TrekDetailResponse(
                trek(TrekStatus.ACTIVE),
                List.of(new CheckpointResponse(UUID.randomUUID(), 1, "Dehradun", 0))));

    mockMvc
        .perform(get("/api/treks/" + TREK_ID).with(ops()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.trek.name").value("Kedarkantha Trek"))
        .andExpect(jsonPath("$.checkpoints[0].name").value("Dehradun"))
        .andExpect(jsonPath("$.checkpoints[0].expectedAfterHours").value(0));
  }

  @Test
  void adminAddsACheckpointAndGets201() throws Exception {
    when(trekService.addCheckpoint(TREK_ID, "Sankri", 2, 8))
        .thenReturn(new CheckpointResponse(UUID.randomUUID(), 2, "Sankri", 8));

    mockMvc
        .perform(
            post("/api/treks/" + TREK_ID + "/checkpoints")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sankri\",\"sequenceNo\":2,\"expectedAfterHours\":8}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sequenceNo").value(2));
  }

  @Test
  void negativeHoursAndZeroSequenceAre400() throws Exception {
    mockMvc
        .perform(
            post("/api/treks/" + TREK_ID + "/checkpoints")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Bad\",\"sequenceNo\":0,\"expectedAfterHours\":-1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.sequenceNo").exists())
        .andExpect(jsonPath("$.errors.expectedAfterHours").exists());
  }

  @Test
  void unknownTrekIs404() throws Exception {
    when(trekService.get(TREK_ID)).thenThrow(new ResourceNotFoundException("Trek not found"));

    mockMvc.perform(get("/api/treks/" + TREK_ID).with(ops())).andExpect(status().isNotFound());
  }

  @Test
  void invalidStatusValueIs400() throws Exception {
    mockMvc
        .perform(
            patch("/api/treks/" + TREK_ID + "/status")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BOGUS\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void forbiddenStatusChangeIs422() throws Exception {
    when(trekService.changeStatus(eq(TREK_ID), eq(TrekStatus.COMPLETED)))
        .thenThrow(
            new BusinessRuleException("Trek status cannot change from PLANNED to COMPLETED"));

    mockMvc
        .perform(
            patch("/api/treks/" + TREK_ID + "/status")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message").value("Trek status cannot change from PLANNED to COMPLETED"));
  }

  @Test
  void operationsCannotAddCheckpoints() throws Exception {
    mockMvc
        .perform(
            post("/api/treks/" + TREK_ID + "/checkpoints")
                .with(ops())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Sankri\",\"sequenceNo\":2,\"expectedAfterHours\":8}"))
        .andExpect(status().isForbidden());
  }
}
