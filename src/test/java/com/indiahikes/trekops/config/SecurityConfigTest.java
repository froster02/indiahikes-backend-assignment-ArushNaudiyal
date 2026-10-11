package com.indiahikes.trekops.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.domain.UserRole;
import com.indiahikes.trekops.service.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Checks the security rules with a throwaway controller, since the real APIs come later. */
@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import({
  SecurityConfig.class,
  JwtService.class,
  SecurityConfigTest.TestClockConfig.class,
  SecurityConfigTest.ProbeController.class
})
@EnableConfigurationProperties(JwtProperties.class)
@TestPropertySource(
    properties = {
      "app.security.jwt-secret=test-secret-test-secret-test-secret-0123456789",
      "app.security.token-ttl-minutes=60"
    })
class SecurityConfigTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtService jwtService;

  @RestController
  static class ProbeController {
    @GetMapping("/api/probe")
    String probe() {
      return "ok";
    }

    @PostMapping("/api/auth/login")
    String login() {
      return "ok";
    }

    @GetMapping("/api/health")
    String health() {
      return "ok";
    }

    @PostMapping({"/api/treks", "/api/treks/1/checkpoints"})
    String adminCreate() {
      return "ok";
    }

    @PatchMapping("/api/treks/1/status")
    String adminStatus() {
      return "ok";
    }
  }

  static class TestClockConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-10T00:30:00Z"), ZoneOffset.UTC);
    }
  }

  private String bearer(UserRole role) {
    return "Bearer " + jwtService.generateToken("someone", role);
  }

  @Test
  void loginAndHealthAreOpenWithoutAToken() throws Exception {
    mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
    mockMvc.perform(get("/api/health")).andExpect(status().isOk());
  }

  @Test
  void protectedPathWithoutATokenReturns401() throws Exception {
    mockMvc.perform(get("/api/probe")).andExpect(status().isUnauthorized());
  }

  @Test
  void protectedPathWithAGarbageTokenReturns401() throws Exception {
    mockMvc
        .perform(get("/api/probe").header("Authorization", "Bearer garbage"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void protectedPathWithAnExpiredTokenReturns401() throws Exception {
    Clock longAgo = Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC);
    JwtService oldIssuer =
        new JwtService(
            new JwtProperties("test-secret-test-secret-test-secret-0123456789", 60), longAgo);
    String expired = "Bearer " + oldIssuer.generateToken("someone", UserRole.ADMIN);

    mockMvc
        .perform(get("/api/probe").header("Authorization", expired))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void bothRolesCanReachOrdinaryProtectedPaths() throws Exception {
    mockMvc
        .perform(get("/api/probe").header("Authorization", bearer(UserRole.OPERATIONS)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/probe").header("Authorization", bearer(UserRole.ADMIN)))
        .andExpect(status().isOk());
  }

  @Test
  void operationsCannotCreateTreksOrCheckpointsOrChangeTrekStatus() throws Exception {
    String ops = bearer(UserRole.OPERATIONS);
    mockMvc
        .perform(post("/api/treks").header("Authorization", ops))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(post("/api/treks/1/checkpoints").header("Authorization", ops))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(patch("/api/treks/1/status").header("Authorization", ops))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCanCreateTreksOrCheckpointsAndChangeTrekStatus() throws Exception {
    String admin = bearer(UserRole.ADMIN);
    mockMvc.perform(post("/api/treks").header("Authorization", admin)).andExpect(status().isOk());
    mockMvc
        .perform(post("/api/treks/1/checkpoints").header("Authorization", admin))
        .andExpect(status().isOk());
    mockMvc
        .perform(patch("/api/treks/1/status").header("Authorization", admin))
        .andExpect(status().isOk());
  }
}
