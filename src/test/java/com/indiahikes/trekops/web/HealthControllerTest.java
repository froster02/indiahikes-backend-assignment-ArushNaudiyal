package com.indiahikes.trekops.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.SecurityConfig;
import com.indiahikes.trekops.config.SecurityErrorHandler;
import com.indiahikes.trekops.service.HealthService;
import com.indiahikes.trekops.service.JwtService;
import com.indiahikes.trekops.web.dto.HealthResponse;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class HealthControllerTest {

  private static final Instant NOW = Instant.parse("2026-08-10T00:30:00Z");

  @Autowired private MockMvc mockMvc;
  @MockBean private HealthService healthService;
  @MockBean private JwtService jwtService;

  @Test
  void isPublicAndReturns200WhenUp() throws Exception {
    when(healthService.check())
        .thenReturn(new HealthResponse("UP", "trekops", "0.0.1-SNAPSHOT", "UP", NOW));

    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.application").value("trekops"))
        .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
        .andExpect(jsonPath("$.database").value("UP"));
  }

  @Test
  void returns503WhenTheDatabaseIsDown() throws Exception {
    when(healthService.check())
        .thenReturn(new HealthResponse("DOWN", "trekops", "0.0.1-SNAPSHOT", "DOWN", NOW));

    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.database").value("DOWN"));
  }
}
