package com.indiahikes.trekops.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indiahikes.trekops.domain.UserRole;
import com.indiahikes.trekops.service.DelayDetectionService;
import com.indiahikes.trekops.service.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Starts the whole application on H2 with the real Flyway migrations, security, BCrypt seed
 * passwords and transactions. Catches what slices and mocks cannot: schema and entity mismatches,
 * wiring errors, and the flow across layers. Uses its own in-memory database and a fixed clock of
 * 2026-08-10 10:00 UTC, when the seed trek's Dehradun and Sankri checkpoints are overdue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = "spring.datasource.url=jdbc:h2:mem:smoke;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ExtendWith(OutputCaptureExtension.class)
class SmokeIntegrationTest {

  private static final String AARAV = "d0000000-0000-0000-0000-000000000001";
  private static final String DEHRADUN = "c0000000-0000-0000-0000-000000000001";

  @TestConfiguration
  static class FixedClockConfig {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-08-10T10:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JwtService jwtService;
  @Autowired private DelayDetectionService delayDetectionService;

  private String login(String username, String password) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return "Bearer " + objectMapper.readTree(body).get("token").asText();
  }

  @Test
  void flywayAppliesBothMigrations() {
    Integer applied =
        jdbcTemplate.queryForObject(
            "select count(*) from \"flyway_schema_history\""
                + " where \"type\" = 'SQL' and \"success\" = true",
            Integer.class);
    assertThat(applied).isEqualTo(2);
  }

  @Test
  void seededUsersLogInWithTheirRole() throws Exception {
    String admin = login("admin", "Admin@12345").substring("Bearer ".length());
    String ops = login("ops", "Ops@12345").substring("Bearer ".length());

    assertThat(jwtService.parseToken(admin).role()).isEqualTo(UserRole.ADMIN);
    assertThat(jwtService.parseToken(ops).role()).isEqualTo(UserRole.OPERATIONS);
  }

  @Test
  void wrongPasswordGets401WithTheErrorBody() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"))
        .andExpect(jsonPath("$.path").value("/api/auth/login"));
  }

  @Test
  void endToEndFlowFromCheckInToResolvedAlert(CapturedOutput output) throws Exception {
    String ops = login("ops", "Ops@12345");

    // Public health reports the H2 database as up.
    mockMvc.perform(get("/api/health")).andExpect(jsonPath("$.database").value("UP"));

    // OPERATIONS may not create treks, but may read them.
    mockMvc
        .perform(
            post("/api/treks")
                .header("Authorization", ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"name\":\"X\",\"startAt\":\"2026-09-01T00:00:00Z\","
                        + "\"endAt\":\"2026-09-02T00:00:00Z\"}"))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/treks").header("Authorization", ops))
        .andExpect(jsonPath("$.content[0].name").value("Kedarkantha Trek"));

    // Aarav reports at Dehradun; a second report is a conflict.
    String checkIn = "{\"participantId\":\"" + AARAV + "\",\"checkpointId\":\"" + DEHRADUN + "\"}";
    mockMvc
        .perform(
            post("/api/check-ins")
                .header("Authorization", ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkIn))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.recordedBy").value("ops"));
    mockMvc
        .perform(
            post("/api/check-ins")
                .header("Authorization", ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkIn))
        .andExpect(status().isConflict());

    // At 10:00 Aarav is late at Sankri; Diya is late at Dehradun and Sankri.
    assertThat(delayDetectionService.detectDelays()).hasSize(3);
    assertThat(delayDetectionService.detectDelays()).isEmpty();
    // The log channel announced the alerts after the transaction committed.
    assertThat(output).contains("Aarav Sharma (+91 90000 00001) has not reported at Sankri");

    String alerts =
        mockMvc
            .perform(get("/api/alerts").param("status", "OPEN").header("Authorization", ops))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode first = objectMapper.readTree(alerts).get("content").get(0);
    String alertId = first.get("id").asText();

    mockMvc
        .perform(post("/api/alerts/" + alertId + "/acknowledge").header("Authorization", ops))
        .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
        .andExpect(jsonPath("$.acknowledgedBy").value("ops"));
    mockMvc
        .perform(post("/api/alerts/" + alertId + "/resolve").header("Authorization", ops))
        .andExpect(jsonPath("$.status").value("RESOLVED"));

    // The audit columns hold the real user for API writes.
    String recordedBy =
        jdbcTemplate.queryForObject(
            "select created_by from check_ins where participant_id = ?",
            String.class,
            java.util.UUID.fromString(AARAV));
    assertThat(recordedBy).isEqualTo("ops");
  }
}
