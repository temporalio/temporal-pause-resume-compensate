package io.temporal.sample.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.sample.activities.SampleActivities;
import io.temporal.sample.model.SampleInput;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Async;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = "samplequeue")
public class SampleWorkflowImpl implements SampleWorkflow {
    private Logger logger = Workflow.getLogger(SampleWorkflowImpl.class);
    private SampleActivities activities = Workflow.newActivityStub(SampleActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setHeartbeatTimeout(Duration.ofSeconds(3))
                    .build());
    // Activities promise
    private Promise<Void> activitiesPromise;

    @Override
    public String run(SampleInput input) {

        // Create the timer promise
        Promise<Void> timerPromise = Workflow.newTimer(Duration.ofSeconds(input.getTimer()));

        // Create cancellation scope for activities
        CancellationScope scope =
                Workflow.newCancellationScope(
                        () -> {
                            activitiesPromise = Async.procedure(this::runActivities);
                        });
        scope.run();

        Promise.anyOf(timerPromise, activitiesPromise).get();
        if(timerPromise.isCompleted()) {
            // cancel activities
            scope.cancel("timer fired");
            return "{\"result\":\"timer completed first..." + input.getTimer() +"\"}";
        } else {
            return "{\"result\":\"activities completed first..." + input.getTimer() +"\"}";
        }
    }

    private void runActivities() {
        activities.one();
        activities.two();
        activities.three();
        activities.four();
    }

}
