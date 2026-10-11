package com.indiahikes.trekops.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationDispatcherTest {

  private static final AlertCreatedEvent EVENT =
      new AlertCreatedEvent(
          UUID.randomUUID(),
          "Aarav Sharma",
          "+91 90000 00001",
          "Kedarkantha Trek",
          "Sankri",
          Instant.parse("2026-08-10T08:30:00Z"),
          Instant.parse("2026-08-10T09:00:00Z"));

  /** A channel that remembers what it received, optionally failing. */
  static class RecordingChannel implements NotificationChannel {
    final String name;
    final boolean fails;
    final List<AlertCreatedEvent> received = new ArrayList<>();

    RecordingChannel(String name, boolean fails) {
      this.name = name;
      this.fails = fails;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public void send(AlertCreatedEvent event) {
      received.add(event);
      if (fails) {
        throw new IllegalStateException("smtp down");
      }
    }
  }

  @Test
  void sendsOnlyToConfiguredChannels() {
    RecordingChannel log = new RecordingChannel("log", false);
    RecordingChannel email = new RecordingChannel("email", false);
    NotificationDispatcher dispatcher =
        new NotificationDispatcher(List.of(log, email), new NotificationProperties(List.of("log")));

    dispatcher.onAlertCreated(EVENT);

    assertThat(log.received).containsExactly(EVENT);
    assertThat(email.received).isEmpty();
    assertThat(dispatcher.enabledChannelNames()).containsExactly("log");
  }

  @Test
  void addingAChannelIsOneClassPlusItsName() {
    RecordingChannel log = new RecordingChannel("log", false);
    RecordingChannel sms = new RecordingChannel("sms", false);
    NotificationDispatcher dispatcher =
        new NotificationDispatcher(
            List.of(log, sms), new NotificationProperties(List.of("log", " sms ")));

    dispatcher.onAlertCreated(EVENT);

    assertThat(log.received).hasSize(1);
    assertThat(sms.received).hasSize(1);
  }

  @Test
  void aFailingChannelDoesNotStopTheOthers() {
    RecordingChannel broken = new RecordingChannel("email", true);
    RecordingChannel log = new RecordingChannel("log", false);
    NotificationDispatcher dispatcher =
        new NotificationDispatcher(
            List.of(broken, log), new NotificationProperties(List.of("email", "log")));

    dispatcher.onAlertCreated(EVENT);

    assertThat(broken.received).hasSize(1);
    assertThat(log.received).hasSize(1);
  }

  @Test
  void unknownOrMissingChannelNamesSendNothing() {
    RecordingChannel log = new RecordingChannel("log", false);

    new NotificationDispatcher(List.of(log), new NotificationProperties(List.of("pager")))
        .onAlertCreated(EVENT);
    new NotificationDispatcher(List.of(log), new NotificationProperties(null))
        .onAlertCreated(EVENT);

    assertThat(log.received).isEmpty();
  }
}
