package com.indiahikes.trekops.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.SecurityConfig;
import com.indiahikes.trekops.config.SecurityErrorHandler;
import com.indiahikes.trekops.domain.AlertStatus;
import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.service.AlertService;
import com.indiahikes.trekops.service.JwtService;
import com.indiahikes.trekops.web.dto.AlertResponse;
import com.indiahikes.trekops.web.dto.PageResponse;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AlertController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class AlertControllerTest {

  private static final UUID ALERT_ID = UUID.fromString("e0000000-0000-0000-0000-000000000001");
  private static final Instant T = Instant.parse("2026-08-10T12:00:00Z");

  @Autowired private MockMvc mockMvc;
  @MockBean private AlertService alertService;
  @MockBean private JwtService jwtService;

  private static AlertResponse alert(AlertStatus status, String ackBy) {
    return new AlertResponse(
        ALERT_ID,
        status,
        UUID.randomUUID(),
        "Aarav Sharma",
        UUID.randomUUID(),
        "Kedarkantha Trek",
        UUID.randomUUID(),
        "Sankri",
        2,
        T,
        T,
        ackBy,
        ackBy == null ? null : T,
        null,
        null);
  }

  @Test
  void listsOpenAlertsNewestFirstByDefault() throws Exception {
    when(alertService.list(eq(AlertStatus.OPEN), any()))
        .thenReturn(new PageResponse<>(List.of(alert(AlertStatus.OPEN, null)), 0, 20, 1, 1));

    mockMvc
        .perform(get("/api/alerts").param("status", "OPEN").with(user("ops").roles("OPERATIONS")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].checkpointName").value("Sankri"))
        .andExpect(jsonPath("$.content[0].status").value("OPEN"));

    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(alertService).list(eq(AlertStatus.OPEN), pageable.capture());
    org.assertj.core.api.Assertions.assertThat(pageable.getValue().getSort())
        .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
  }

  @Test
  void listWithoutStatusPassesNull() throws Exception {
    when(alertService.list(isNull(), any())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

    mockMvc
        .perform(get("/api/alerts").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void operationsAcknowledgesAsTheLoggedInUser() throws Exception {
    when(alertService.acknowledge(ALERT_ID, "ops"))
        .thenReturn(alert(AlertStatus.ACKNOWLEDGED, "ops"));

    mockMvc
        .perform(
            post("/api/alerts/" + ALERT_ID + "/acknowledge").with(user("ops").roles("OPERATIONS")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
        .andExpect(jsonPath("$.acknowledgedBy").value("ops"));
  }

  @Test
  void adminMayResolve() throws Exception {
    when(alertService.resolve(ALERT_ID, "admin")).thenReturn(alert(AlertStatus.RESOLVED, null));

    mockMvc
        .perform(post("/api/alerts/" + ALERT_ID + "/resolve").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESOLVED"));
  }

  @Test
  void invalidTransitionIs422() throws Exception {
    when(alertService.resolve(ALERT_ID, "ops"))
        .thenThrow(new BusinessRuleException("Alert is already RESOLVED"));

    mockMvc
        .perform(post("/api/alerts/" + ALERT_ID + "/resolve").with(user("ops").roles("OPERATIONS")))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void unknownStatusFilterIs400() throws Exception {
    mockMvc
        .perform(get("/api/alerts").param("status", "LOST").with(user("ops").roles("OPERATIONS")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void withoutATokenIs401() throws Exception {
    mockMvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());
  }
}
