package com.indiahikes.trekops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.indiahikes.trekops.web.dto.HealthResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Properties;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

class HealthServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-10T00:30:00Z");

  private final DataSource dataSource = mock(DataSource.class);
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

  @SuppressWarnings("unchecked")
  private HealthService service(BuildProperties build) {
    ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(build);
    return new HealthService(dataSource, "trekops", provider, clock);
  }

  private static BuildProperties build(String version) {
    Properties properties = new Properties();
    properties.setProperty("version", version);
    return new BuildProperties(properties);
  }

  @Test
  void reportsUpWhenTheDatabaseAnswers() throws SQLException {
    Connection connection = mock(Connection.class);
    when(connection.isValid(2)).thenReturn(true);
    when(dataSource.getConnection()).thenReturn(connection);

    HealthResponse health = service(build("1.2.3")).check();

    assertThat(health.status()).isEqualTo("UP");
    assertThat(health.database()).isEqualTo("UP");
    assertThat(health.application()).isEqualTo("trekops");
    assertThat(health.version()).isEqualTo("1.2.3");
    assertThat(health.time()).isEqualTo(NOW);
  }

  @Test
  void reportsDownWhenTheDatabaseFails() throws SQLException {
    when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));

    HealthResponse health = service(null).check();

    assertThat(health.status()).isEqualTo("DOWN");
    assertThat(health.database()).isEqualTo("DOWN");
    assertThat(health.version()).isEqualTo("unknown");
  }
}
