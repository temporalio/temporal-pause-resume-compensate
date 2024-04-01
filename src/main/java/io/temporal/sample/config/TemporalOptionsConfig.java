package io.temporal.sample.config;

import com.uber.m3.util.ImmutableMap;
import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.taskqueue.v1.TaskQueue;
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.spring.boot.TemporalOptionsCustomizer;
import io.temporal.spring.boot.WorkerOptionsCustomizer;
import io.temporal.worker.NonDeterministicException;
import io.temporal.worker.WorkerOptions;

import javax.annotation.Nonnull;

import io.temporal.worker.WorkflowImplementationOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
        return new TemporalOptionsCustomizer<>() {
            @Nonnull
            @Override
            public WorkflowServiceStubsOptions.Builder customize(
                    @Nonnull WorkflowServiceStubsOptions.Builder optionsBuilder) {
                optionsBuilder.setRpcLongPollTimeout(Duration.ofSeconds(20));
                return optionsBuilder;
            }
        };
    }

    @Bean
    public TemporalOptionsCustomizer<WorkflowImplementationOptions.Builder>
    customWorkflowImplementationOptions() {
        return new TemporalOptionsCustomizer<>() {
            @Nonnull
            @Override
            public WorkflowImplementationOptions.Builder customize(
                    @Nonnull WorkflowImplementationOptions.Builder optionsBuilder) {

                // shows how to fail workflow execution on non-deterministic error
                //optionsBuilder.setFailWorkflowExceptionTypes(NonDeterministicException.class);

                // shows how to fail workflow execution on any workflow error
                //optionsBuilder.setFailWorkflowExceptionTypes(Throwable.class);

                Map<String, ActivityOptions> perActivityOptions = new HashMap<>();
                perActivityOptions.put("One", ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(2))
                        .build());

                perActivityOptions.put("Two", ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(2))
                        .build());

                perActivityOptions.put("Three", ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(2))
                        .build());

                perActivityOptions.put("Four", ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(8))
                        .setHeartbeatTimeout(Duration.ofSeconds(3))
                        .setCancellationType(ActivityCancellationType.TRY_CANCEL)
                        .build());

                perActivityOptions.put("Five", ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(2))
                        .build());

                optionsBuilder.setActivityOptions(perActivityOptions);
                return optionsBuilder;
            }
        };
    }
}
