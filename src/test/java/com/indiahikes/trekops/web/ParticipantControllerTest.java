package com.indiahikes.trekops.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.SecurityConfig;
import com.indiahikes.trekops.config.SecurityErrorHandler;
import com.indiahikes.trekops.domain.ParticipantStatus;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import com.indiahikes.trekops.service.JwtService;
import com.indiahikes.trekops.service.ParticipantService;
import com.indiahikes.trekops.web.dto.PageResponse;
import com.indiahikes.trekops.web.dto.ParticipantResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ParticipantController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class ParticipantControllerTest {

  private static final UUID TREK_ID = UUID.fromString("b0000000-0000-0000-0000-000000000001");
  private static final UUID PARTICIPANT_ID =
      UUID.fromString("d0000000-0000-0000-0000-000000000001");
  private static final Instant NOW = Instant.parse("2026-08-10T00:30:00Z");

  @Autowired private MockMvc mockMvc;
  @MockBean private ParticipantService participantService;
  @MockBean private JwtService jwtService;

  private static RequestPostProcessor ops() {
    return user("ops").roles("OPERATIONS");
  }

  private static ParticipantResponse aarav() {
    return new ParticipantResponse(
        PARTICIPANT_ID,
        TREK_ID,
        "Kedarkantha Trek",
        "Aarav Sharma",
        "+91 90000 00001",
        "+91 90000 10001",
        ParticipantStatus.REGISTERED,
        NOW,
        "ops",
        NOW,
        "ops");
  }

  @Test
  void operationsRegistersAParticipant() throws Exception {
    when(participantService.register(eq(TREK_ID), eq("Aarav Sharma"), any(), any()))
        .thenReturn(aarav());
    String body =
        "{\"trekId\":\""
            + TREK_ID
            + "\",\"name\":\"Aarav Sharma\","
            + "\"phone\":\"+91 90000 00001\",\"emergencyContact\":\"+91 90000 10001\"}";

    mockMvc
        .perform(
            post("/api/participants")
                .with(ops())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/participants/" + PARTICIPANT_ID))
        .andExpect(jsonPath("$.status").value("REGISTERED"));
  }

  @Test
  void badPhoneAndMissingTrekAre400() throws Exception {
    String body = "{\"name\":\"X\",\"phone\":\"call me\",\"emergencyContact\":\"Y\"}";

    mockMvc
        .perform(
            post("/api/participants")
                .with(ops())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.phone").exists())
        .andExpect(jsonPath("$.errors.trekId").exists());
  }

  @Test
  void searchPassesFiltersPageAndSortToTheService() throws Exception {
    when(participantService.search(any(), any(), any(), any(), any()))
        .thenReturn(new PageResponse<>(List.of(aarav()), 0, 5, 1, 1));

    mockMvc
        .perform(
            get("/api/participants")
                .with(ops())
                .param("trekId", TREK_ID.toString())
                .param("status", "ON_TREK")
                .param("name", "sha")
                .param("page", "0")
                .param("size", "5")
                .param("sort", "createdAt,desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].name").value("Aarav Sharma"))
        .andExpect(jsonPath("$.size").value(5));

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(participantService)
        .search(
            eq(TREK_ID), eq(ParticipantStatus.ON_TREK), eq("sha"), isNull(), pageable.capture());
    org.assertj.core.api.Assertions.assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    org.assertj.core.api.Assertions.assertThat(pageable.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  @Test
  void unknownStatusFilterIs400() throws Exception {
    mockMvc
        .perform(get("/api/participants").with(ops()).param("status", "SLEEPING"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void searchWithoutATokenIs401() throws Exception {
    mockMvc.perform(get("/api/participants")).andExpect(status().isUnauthorized());
  }

  @Test
  void unknownParticipantIs404() throws Exception {
    when(participantService.get(PARTICIPANT_ID))
        .thenThrow(new ResourceNotFoundException("Participant not found"));

    mockMvc
        .perform(get("/api/participants/" + PARTICIPANT_ID).with(ops()))
        .andExpect(status().isNotFound());
  }
}
