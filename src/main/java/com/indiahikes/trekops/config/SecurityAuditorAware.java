package com.indiahikes.trekops.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Tells JPA auditing who made a change: the username from the JWT of the current request. Work
 * outside a request, such as the scheduler or a migration, is recorded as "system".
 */
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

  static final String SYSTEM = "system";

  @Override
  public Optional<String> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return Optional.of(SYSTEM);
    }
    return Optional.of(authentication.getName());
  }
}
