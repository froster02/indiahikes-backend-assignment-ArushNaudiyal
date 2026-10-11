package com.indiahikes.trekops.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.SecurityConfig;
import com.indiahikes.trekops.config.SecurityErrorHandler;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.service.CheckInService;
import com.indiahikes.trekops.service.JwtService;
import com.indiahikes.trekops.web.dto.CheckInResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CheckInController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class CheckInControllerTest {

  private static final UUID PARTICIPANT = UUID.fromString("d0000000-0000-0000-0000-000000000001");
  private static final UUID CHECKPOINT = UUID.fromString("c0000000-0000-0000-0000-000000000002");
  private static final Instant ARRIVED = Instant.parse("2026-08-10T08:45:00Z");

  @Autowired private MockMvc mockMvc;
  @MockBean private CheckInService checkInService;
  @MockBean private JwtService jwtService;

  private static String body(String extra) {
    return "{\"participantId\":\""
        + PARTICIPANT
        + "\",\"checkpointId\":\""
        + CHECKPOINT
        + "\""
        + extra
        + "}";
  }

  private static CheckInResponse response() {
    return new CheckInResponse(
        UUID.randomUUID(), PARTICIPANT, CHECKPOINT, "Sankri", 2, ARRIVED, "ops");
  }

  @Test
  void recordsACheckInAsTheLoggedInUser() throws Exception {
    when(checkInService.record(PARTICIPANT, CHECKPOINT, ARRIVED, "ops")).thenReturn(response());

    mockMvc
        .perform(
            post("/api/check-ins")
                .with(user("ops").roles("OPERATIONS"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(",\"checkedInAt\":\"2026-08-10T08:45:00Z\"")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/participants/" + PARTICIPANT))
        .andExpect(jsonPath("$.recordedBy").value("ops"))
        .andExpect(jsonPath("$.checkpointName").value("Sankri"));
  }

  @Test
  void adminMayAlsoRecord() throws Exception {
    when(checkInService.record(eq(PARTICIPANT), eq(CHECKPOINT), isNull(), eq("admin")))
        .thenReturn(response());

    mockMvc
        .perform(
            post("/api/check-ins")
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("")))
        .andExpect(status().isCreated());
    verify(checkInService).record(PARTICIPANT, CHECKPOINT, null, "admin");
  }

  @Test
  void missingIdsAre400() throws Exception {
    mockMvc
        .perform(
            post("/api/check-ins")
                .with(user("ops").roles("OPERATIONS"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.participantId").exists())
        .andExpect(jsonPath("$.errors.checkpointId").exists());
  }

  @Test
  void duplicateIs409() throws Exception {
    when(checkInService.record(eq(PARTICIPANT), eq(CHECKPOINT), isNull(), eq("ops")))
        .thenThrow(new ConflictException("Participant has already checked in at this checkpoint"));

    mockMvc
        .perform(
            post("/api/check-ins")
                .with(user("ops").roles("OPERATIONS"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("")))
        .andExpect(status().isConflict());
  }

  @Test
  void wrongTrekCheckpointIs422() throws Exception {
    when(checkInService.record(eq(PARTICIPANT), eq(CHECKPOINT), isNull(), eq("ops")))
        .thenThrow(
            new BusinessRuleException("Checkpoint does not belong to the participant's trek"));

    mockMvc
        .perform(
            post("/api/check-ins")
                .with(user("ops").roles("OPERATIONS"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(
            jsonPath("$.message").value("Checkpoint does not belong to the participant's trek"));
  }

  @Test
  void withoutATokenIs401() throws Exception {
    mockMvc
        .perform(post("/api/check-ins").contentType(MediaType.APPLICATION_JSON).content(body("")))
        .andExpect(status().isUnauthorized());
  }
}
