package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.indiahikes.trekops.config.JwtProperties;
import com.indiahikes.trekops.domain.UserRole;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";
  private static final Instant ISSUED_AT = Instant.parse("2026-08-10T00:30:00Z");
  private static final long TTL_MINUTES = 60;

  private static JwtService serviceAt(Instant now, String secret) {
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    return new JwtService(new JwtProperties(secret, TTL_MINUTES), clock);
  }

  @Test
  void parsesTheUsernameAndRoleItIssued() {
    JwtService service = serviceAt(ISSUED_AT, SECRET);

    String token = service.generateToken("ops", UserRole.OPERATIONS);
    JwtService.TokenClaims claims = service.parseToken(token);

    assertThat(claims.username()).isEqualTo("ops");
    assertThat(claims.role()).isEqualTo(UserRole.OPERATIONS);
  }

  @Test
  void acceptsATokenJustBeforeItExpires() {
    String token = serviceAt(ISSUED_AT, SECRET).generateToken("admin", UserRole.ADMIN);
    Instant almostExpired = ISSUED_AT.plus(Duration.ofMinutes(TTL_MINUTES - 1));

    assertThat(serviceAt(almostExpired, SECRET).parseToken(token).username()).isEqualTo("admin");
  }

  @Test
  void rejectsAnExpiredToken() {
    String token = serviceAt(ISSUED_AT, SECRET).generateToken("admin", UserRole.ADMIN);
    Instant expired = ISSUED_AT.plus(Duration.ofMinutes(TTL_MINUTES + 1));

    assertThatThrownBy(() -> serviceAt(expired, SECRET).parseToken(token))
        .isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void rejectsATokenSignedWithAnotherSecret() {
    String otherSecret = "another-secret-another-secret-0123456789";
    String token = serviceAt(ISSUED_AT, otherSecret).generateToken("admin", UserRole.ADMIN);

    assertThatThrownBy(() -> serviceAt(ISSUED_AT, SECRET).parseToken(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rejectsAMalformedToken() {
    assertThatThrownBy(() -> serviceAt(ISSUED_AT, SECRET).parseToken("not-a-jwt"))
        .isInstanceOf(JwtException.class);
  }
}
