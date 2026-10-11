package com.indiahikes.trekops.scheduler;

import com.indiahikes.trekops.service.DelayDetectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the delay check on a fixed delay. All logic lives in {@link DelayDetectionService}.
 *
 * <p>Fixed delay means the next run starts {@code interval-ms} after the previous one ends, so runs
 * never overlap. This bean exists only when {@code app.scheduler.delay-check.enabled} is true. It
 * is meant for a single app instance; several instances would each run the check.
 */
@Component
@ConditionalOnProperty(prefix = "app.scheduler.delay-check", name = "enabled", havingValue = "true")
public class DelayDetectionScheduler {

  private final DelayDetectionService delayDetectionService;

  public DelayDetectionScheduler(DelayDetectionService delayDetectionService) {
    this.delayDetectionService = delayDetectionService;
  }

  @Scheduled(fixedDelayString = "${app.scheduler.delay-check.interval-ms}")
  public void run() {
    delayDetectionService.detectDelays();
  }
}
