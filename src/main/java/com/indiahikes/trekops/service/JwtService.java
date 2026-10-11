package com.indiahikes.trekops.service;

import com.indiahikes.trekops.config.JwtProperties;
import com.indiahikes.trekops.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Issues and parses signed JWTs (HS256). Time comes from the injected Clock. */
@Service
public class JwtService {

  private static final String ROLE_CLAIM = "role";

  private final SecretKey key;
  private final Duration ttl;
  private final Clock clock;

  public JwtService(JwtProperties properties, Clock clock) {
    this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    this.ttl = Duration.ofMinutes(properties.tokenTtlMinutes());
    this.clock = clock;
  }

  public Duration getTtl() {
    return ttl;
  }

  /** Creates a token with the username as subject, the role as a claim and an expiry. */
  public String generateToken(String username, UserRole role) {
    Instant now = clock.instant();
    return Jwts.builder()
        .subject(username)
        .claim(ROLE_CLAIM, role.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Checks signature and expiry and returns the claims.
   *
   * @throws JwtException when the token is malformed, tampered with, expired or has no valid role
   */
  public TokenClaims parseToken(String token) {
    Claims claims =
        Jwts.parser()
            .verifyWith(key)
            .clock(() -> Date.from(clock.instant()))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    String role = claims.get(ROLE_CLAIM, String.class);
    if (role == null) {
      throw new JwtException("Token has no role");
    }
    try {
      return new TokenClaims(claims.getSubject(), UserRole.valueOf(role));
    } catch (IllegalArgumentException e) {
      throw new JwtException("Token has an unknown role", e);
    }
  }

  /** What the rest of the app needs from a valid token. */
  public record TokenClaims(String username, UserRole role) {}
}
