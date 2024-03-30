package io.temporal;

import io.temporal.spring.boot.WorkerOptionsCustomizer;
import io.temporal.worker.WorkerOptions;
import javax.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import java.time.Duration;

public class TemporalOptionsConfig {
  // Worker specific options customization
  @Bean
  public WorkerOptionsCustomizer customWorkerOptions() {
    return new WorkerOptionsCustomizer() {
      @Nonnull
      @Override
      public WorkerOptions.Builder customize(
          @Nonnull WorkerOptions.Builder optionsBuilder,
          @Nonnull String workerName,
          @Nonnull String taskQueue) {

        // For CustomizeTaskQueue (also name of worker) we set worker
        // to only handle workflow tasks and local activities
        if (taskQueue.equals("CustomizeTaskQueue")) {
          optionsBuilder.setStickyQueueScheduleToStartTimeout(Duration.ofSeconds(30));
        }
        return optionsBuilder;
      }
    };
  }  
}
