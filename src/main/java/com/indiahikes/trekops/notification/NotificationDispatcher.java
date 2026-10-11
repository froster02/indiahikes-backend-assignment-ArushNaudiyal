package com.indiahikes.trekops.notification;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends each new alert to every enabled channel. It runs after the alert's transaction commits, so
 * a rolled back alert is never announced. A failing channel is logged and does not stop the others.
 */
@Component
public class NotificationDispatcher {

  private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

  private final List<NotificationChannel> enabledChannels;

  public NotificationDispatcher(
      List<NotificationChannel> availableChannels, NotificationProperties properties) {
    Set<String> wanted =
        properties.channels() == null
            ? Set.of()
            : properties.channels().stream().map(String::trim).collect(Collectors.toSet());
    this.enabledChannels =
        availableChannels.stream().filter(channel -> wanted.contains(channel.name())).toList();
    Set<String> known =
        availableChannels.stream().map(NotificationChannel::name).collect(Collectors.toSet());
    wanted.stream()
        .filter(name -> !known.contains(name))
        .forEach(name -> log.warn("Unknown notification channel '{}' is ignored", name));
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onAlertCreated(AlertCreatedEvent event) {
    for (NotificationChannel channel : enabledChannels) {
      try {
        channel.send(event);
      } catch (RuntimeException e) {
        log.error(
            "Notification channel '{}' failed for alert {}", channel.name(), event.alertId(), e);
      }
    }
  }

  /** Names of the channels in use. */
  public List<String> enabledChannelNames() {
    return enabledChannels.stream().map(NotificationChannel::name).toList();
  }
}
