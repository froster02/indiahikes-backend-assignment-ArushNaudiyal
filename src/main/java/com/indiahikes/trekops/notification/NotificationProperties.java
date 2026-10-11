package com.indiahikes.trekops.notification;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code app.notifications.channels}: comma separated channel names, for example "log". */
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(List<String> channels) {}
