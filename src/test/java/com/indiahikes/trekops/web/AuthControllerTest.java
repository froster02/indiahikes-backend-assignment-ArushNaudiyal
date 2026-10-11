package com.indiahikes.trekops.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.SecurityConfig;
import com.indiahikes.trekops.config.SecurityErrorHandler;
import com.indiahikes.trekops.service.AuthService;
import com.indiahikes.trekops.service.JwtService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, SecurityErrorHandler.class, ClockConfig.class})
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private AuthService authService;
  // SecurityConfig builds the JWT filter from this service.
  @MockBean private JwtService jwtService;

  private static final String VALID_BODY = "{\"username\":\"ops\",\"password\":\"Ops@12345\"}";

  @Test
  void validLoginReturnsTheToken() throws Exception {
    when(authService.login("ops", "Ops@12345"))
        .thenReturn(new AuthService.LoginResult("the.jwt.token", Duration.ofMinutes(60)));

    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("the.jwt.token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresInSeconds").value(3600));
  }

  @Test
  void badCredentialsReturn401() throws Exception {
    when(authService.login(anyString(), anyString()))
        .thenThrow(new BadCredentialsException("Invalid username or password"));

    mockMvc
        .perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password"))
        .andExpect(jsonPath("$.path").value("/api/auth/login"));
  }

  @Test
  void blankUsernameReturns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.errors.username").exists());
  }
}
