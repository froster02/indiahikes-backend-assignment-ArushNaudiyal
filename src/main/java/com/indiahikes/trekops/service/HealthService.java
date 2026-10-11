package com.indiahikes.trekops.service;

import com.indiahikes.trekops.web.dto.HealthResponse;
import java.sql.Connection;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/** Basic application information for the public health endpoint. */
@Service
public class HealthService {

  private static final int DB_TIMEOUT_SECONDS = 2;

  private final DataSource dataSource;
  private final String applicationName;
  private final String version;
  private final Clock clock;

  public HealthService(
      DataSource dataSource,
      @Value("${spring.application.name}") String applicationName,
      ObjectProvider<BuildProperties> buildProperties,
      Clock clock) {
    this.dataSource = dataSource;
    this.applicationName = applicationName;
    BuildProperties build = buildProperties.getIfAvailable();
    this.version = build != null ? build.getVersion() : "unknown";
    this.clock = clock;
  }

  public HealthResponse check() {
    boolean databaseUp = isDatabaseUp();
    return new HealthResponse(
        databaseUp ? "UP" : "DOWN",
        applicationName,
        version,
        databaseUp ? "UP" : "DOWN",
        clock.instant());
  }

  private boolean isDatabaseUp() {
    try (Connection connection = dataSource.getConnection()) {
      return connection.isValid(DB_TIMEOUT_SECONDS);
    } catch (Exception e) {
      return false;
    }
  }
}
