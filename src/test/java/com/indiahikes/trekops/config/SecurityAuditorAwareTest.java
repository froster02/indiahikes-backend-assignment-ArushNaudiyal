package com.indiahikes.trekops.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityAuditorAwareTest {

  private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usesTheUsernameOfTheAuthenticatedUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "ops", null, List.of(new SimpleGrantedAuthority("ROLE_OPERATIONS"))));

    assertThat(auditorAware.getCurrentAuditor()).contains("ops");
  }

  @Test
  void fallsBackToSystemWhenNobodyIsAuthenticated() {
    assertThat(auditorAware.getCurrentAuditor()).contains("system");
  }

  @Test
  void fallsBackToSystemForAnAnonymousUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(auditorAware.getCurrentAuditor()).contains("system");
  }
}
