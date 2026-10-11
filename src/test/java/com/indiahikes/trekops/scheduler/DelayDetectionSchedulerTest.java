package com.indiahikes.trekops.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.indiahikes.trekops.service.DelayDetectionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DelayDetectionSchedulerTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withBean(DelayDetectionService.class, () -> mock(DelayDetectionService.class))
          .withUserConfiguration(DelayDetectionScheduler.class);

  @Test
  void eachRunCallsTheService() {
    DelayDetectionService service = mock(DelayDetectionService.class);

    new DelayDetectionScheduler(service).run();

    verify(service).detectDelays();
  }

  @Test
  void existsWhenEnabled() {
    contextRunner
        .withPropertyValues("app.scheduler.delay-check.enabled=true")
        .run(context -> assertThat(context).hasSingleBean(DelayDetectionScheduler.class));
  }

  @Test
  void isAbsentWhenDisabled() {
    contextRunner
        .withPropertyValues("app.scheduler.delay-check.enabled=false")
        .run(context -> assertThat(context).doesNotHaveBean(DelayDetectionScheduler.class));
  }
}
