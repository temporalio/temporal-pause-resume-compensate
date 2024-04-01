package io.temporal.sample.config;

import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import io.temporal.spring.boot.WorkerOptionsCustomizer;
import io.temporal.worker.WorkerOptions;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class TemporalOptionsConfig {
    @Bean
    public WorkerOptionsCustomizer customWorkerOptions() {
        return new WorkerOptionsCustomizer() {
            @Nonnull
            @Override
            public WorkerOptions.Builder customize(
                    @Nonnull WorkerOptions.Builder optionsBuilder,
                    @Nonnull String workerName,
                    @Nonnull String taskQueue) {
                optionsBuilder.setStickyQueueScheduleToStartTimeout(Duration.ofSeconds(25));
                return optionsBuilder;
            }
        };
    }

    @Bean
    public TemporalOptionsCustomizer<WorkflowServiceStubsOptions.Builder>
    customServiceStubsOptions() {
        return new TemporalOptionsCustomizer<WorkflowServiceStubsOptions.Builder>() {
            @Nonnull
            @Override
            public WorkflowServiceStubsOptions.Builder customize(
                    @Nonnull WorkflowServiceStubsOptions.Builder optionsBuilder) {
                optionsBuilder.setRpcLongPollTimeout(Duration.ofSeconds(20));
                return optionsBuilder;
            }
        };
    }
}
