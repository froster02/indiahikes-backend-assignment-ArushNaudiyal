package com.indiahikes.trekops.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Writes the notification to the application log. Stands in for real delivery. */
@Component
public class LogNotificationChannel implements NotificationChannel {

  private static final Logger log = LoggerFactory.getLogger(LogNotificationChannel.class);

  @Override
  public String name() {
    return "log";
  }

  @Override
  public void send(AlertCreatedEvent event) {
    log.warn(
        "ALERT {}: {} ({}) has not reported at {} on {}; expected at {}",
        event.alertId(),
        event.participantName(),
        event.participantPhone(),
        event.checkpointName(),
        event.trekName(),
        event.expectedAt());
  }
}
