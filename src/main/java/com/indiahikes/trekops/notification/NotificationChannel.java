package com.indiahikes.trekops.notification;

/**
 * One way of telling operations users about a new alert. To add a channel such as email or SMS,
 * write one class that implements this interface and is a Spring bean, then add its name to
 * NOTIFICATION_CHANNELS.
 */
public interface NotificationChannel {

  /** The name used in {@code app.notifications.channels}, for example "log". */
  String name();

  void send(AlertCreatedEvent event);
}
