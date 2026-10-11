package com.indiahikes.trekops.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.indiahikes.trekops.common.ClockConfig;
import com.indiahikes.trekops.config.JpaAuditingConfig;
import com.indiahikes.trekops.config.SecurityAuditorAware;
import com.indiahikes.trekops.domain.Trek;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the real Flyway migrations on H2 (test profile) and checks that audit fields are filled on
 * save. This is the first time the app schema and seed data run on H2.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, ClockConfig.class, SecurityAuditorAware.class})
class AuditingRepositoryTest {

  @Autowired private TrekRepository trekRepository;
  @Autowired private UserRepository userRepository;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void migrationsLoadTheSeedUsersOnH2() {
    assertThat(userRepository.findByUsername("admin")).isPresent();
    assertThat(userRepository.findByUsername("ops")).isPresent();
  }

  @Test
  void saveRecordsTheAuthenticatedUserAndTimes() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

    Trek trek =
        trekRepository.saveAndFlush(
            new Trek(
                "Test Trek",
                Instant.parse("2027-01-01T00:00:00Z"),
                Instant.parse("2027-01-04T00:00:00Z")));

    assertThat(trek.getCreatedBy()).isEqualTo("admin");
    assertThat(trek.getUpdatedBy()).isEqualTo("admin");
    assertThat(trek.getCreatedAt()).isNotNull();
    assertThat(trek.getUpdatedAt()).isNotNull();
  }

  @Test
  void saveWithoutALoggedInUserRecordsSystem() {
    Trek trek =
        trekRepository.saveAndFlush(
            new Trek(
                "Background Trek",
                Instant.parse("2027-02-01T00:00:00Z"),
                Instant.parse("2027-02-04T00:00:00Z")));

    assertThat(trek.getCreatedBy()).isEqualTo("system");
  }
}
