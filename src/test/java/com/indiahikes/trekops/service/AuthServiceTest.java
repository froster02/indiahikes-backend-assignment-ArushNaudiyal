package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.indiahikes.trekops.config.JwtProperties;
import com.indiahikes.trekops.domain.User;
import com.indiahikes.trekops.domain.UserRole;
import com.indiahikes.trekops.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();
  private final UserRepository userRepository = mock(UserRepository.class);
  private JwtService jwtService;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-08-10T00:30:00Z"), ZoneOffset.UTC);
    jwtService =
        new JwtService(
            new JwtProperties("test-secret-test-secret-test-secret-0123456789", 60), clock);
    authService = new AuthService(userRepository, encoder, jwtService);
    when(userRepository.findByUsername("ops"))
        .thenReturn(Optional.of(new User("ops", encoder.encode("Ops@12345"), UserRole.OPERATIONS)));
    when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());
  }

  @Test
  void validCredentialsReturnATokenForThatUserAndRole() {
    AuthService.LoginResult result = authService.login("ops", "Ops@12345");

    JwtService.TokenClaims claims = jwtService.parseToken(result.token());
    assertThat(claims.username()).isEqualTo("ops");
    assertThat(claims.role()).isEqualTo(UserRole.OPERATIONS);
    assertThat(result.expiresIn()).isEqualTo(Duration.ofMinutes(60));
  }

  @Test
  void wrongPasswordIsRejected() {
    assertThatThrownBy(() -> authService.login("ops", "wrong"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid username or password");
  }

  @Test
  void unknownUserIsRejectedWithTheSameMessage() {
    assertThatThrownBy(() -> authService.login("nobody", "Ops@12345"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Invalid username or password");
  }
}
