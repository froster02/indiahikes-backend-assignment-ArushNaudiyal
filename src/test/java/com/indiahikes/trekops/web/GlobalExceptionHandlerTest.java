package com.indiahikes.trekops.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.exception.BusinessRuleException;
import com.indiahikes.trekops.exception.ConflictException;
import com.indiahikes.trekops.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Every exception must come back as the same ApiError shape. A probe controller throws them. */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ProbeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
  GlobalExceptionHandlerTest.ProbeController.class,
  GlobalExceptionHandlerTest.TestClockConfig.class
})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  record SampleRequest(@NotBlank String name, @Min(1) int age) {}

  @RestController
  static class ProbeController {
    @GetMapping("/probe/not-found")
    void notFound() {
      throw new ResourceNotFoundException("Trek 42 not found");
    }

    @GetMapping("/probe/conflict")
    void conflict() {
      throw new ConflictException("Participant already checked in");
    }

    @GetMapping("/probe/rule")
    void rule() {
      throw new BusinessRuleException("Checkpoint belongs to another trek");
    }

    @GetMapping("/probe/denied")
    void denied() {
      throw new AccessDeniedException("internal detail");
    }

    @GetMapping("/probe/bad-credentials")
    void badCredentials() {
      throw new BadCredentialsException("Invalid username or password");
    }

    @GetMapping("/probe/boom")
    void boom() {
      throw new IllegalStateException("secret internal detail");
    }

    @GetMapping("/probe/number/{n}")
    String number(@PathVariable int n) {
      return "ok";
    }

    @PostMapping("/probe/validate")
    String validate(@Valid @RequestBody SampleRequest request) {
      return "ok";
    }
  }

  static class TestClockConfig {
    @Bean
    Clock clock() {
      return Clock.fixed(Instant.parse("2026-08-10T00:30:00Z"), ZoneOffset.UTC);
    }
  }

  @Test
  void notFoundIs404WithTheMessageAndPath() throws Exception {
    mockMvc
        .perform(get("/probe/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.timestamp").value("2026-08-10T00:30:00Z"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Trek 42 not found"))
        .andExpect(jsonPath("$.path").value("/probe/not-found"))
        .andExpect(jsonPath("$.errors").doesNotExist());
  }

  @Test
  void conflictIs409() throws Exception {
    mockMvc
        .perform(get("/probe/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.message").value("Participant already checked in"));
  }

  @Test
  void businessRuleViolationIs422() throws Exception {
    mockMvc
        .perform(get("/probe/rule"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.message").value("Checkpoint belongs to another trek"));
  }

  @Test
  void validationFailureIs400WithAMapOfBadFields() throws Exception {
    mockMvc
        .perform(
            post("/probe/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"age\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.age").exists());
  }

  @Test
  void malformedJsonIs400() throws Exception {
    mockMvc
        .perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON).content("{oops"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed request"));
  }

  @Test
  void wrongTypeInPathIs400() throws Exception {
    mockMvc
        .perform(get("/probe/number/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed request"));
  }

  @Test
  void badCredentialsIs401() throws Exception {
    mockMvc
        .perform(get("/probe/bad-credentials"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"));
  }

  @Test
  void accessDeniedIs403WithoutInternalDetail() throws Exception {
    mockMvc
        .perform(get("/probe/denied"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("Access denied"));
  }

  @Test
  void unknownPathIs404() throws Exception {
    mockMvc
        .perform(get("/no/such/path"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Resource not found"));
  }

  @Test
  void wrongMethodIs405() throws Exception {
    mockMvc
        .perform(post("/probe/not-found"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.status").value(405));
  }

  @Test
  void unexpectedErrorIs500AndHidesTheDetail() throws Exception {
    mockMvc
        .perform(get("/probe/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.message").value("Unexpected error"));
  }
}
