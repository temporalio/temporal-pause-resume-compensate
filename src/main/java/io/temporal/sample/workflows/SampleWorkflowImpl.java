package io.temporal.sample.workflows;

import io.temporal.activity.ActivityCancellationType;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowServiceException;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.sample.activities.SampleActivities;
import io.temporal.sample.model.SampleInput;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.*;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = "samplequeue")
public class SampleWorkflowImpl implements SampleWorkflow {
    private Logger logger = Workflow.getLogger(SampleWorkflowImpl.class);
    private SampleActivities activities = Workflow.newActivityStub(SampleActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(5))
                    .setHeartbeatTimeout(Duration.ofSeconds(3))
                    // set needed cancellation type
                    .setCancellationType(ActivityCancellationType.TRY_CANCEL)
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

        // Wait for timer and activities promises, whichever completes first
        Promise.anyOf(timerPromise, activitiesPromise).get();

        if (timerPromise.isCompleted()) {
            scope.cancel("timer fired...");
            SampleCleanupWorkflow cleanupChild =
                    Workflow.newChildWorkflowStub(SampleCleanupWorkflow.class,
                            ChildWorkflowOptions.newBuilder()
                                    .setTaskQueue("samplecleanupqueue")
                                    .setWorkflowId(Workflow.getInfo().getWorkflowId() + "-cleanup")
                                    .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                                    .build());

            try {
                // start child async, wait until it starts (not completes)
                Async.function(cleanupChild::cleanup, input);
                Workflow.getWorkflowExecution(cleanupChild).get();
            } catch (ChildWorkflowFailure e) {
                if (e.getCause() instanceof WorkflowExecutionAlreadyStarted) {
                    // one reason could be workflow id reuse policy
                    logger.error("Child workflow execution already started: " + e.getCause().getMessage());
                } else if (e.getCause() instanceof WorkflowServiceException) {
                    // could be some other type of service exception
                    logger.error("Service exception starting child: " + e.getCause().getMessage());
                } else {
                    // something else?
                    logger.error("Exception starting child: " + e.getCause().getMessage());
                }
            }

            // client result
            return "{\"result\":\"timer completed first..." + input.getTimer() + "\"}";
        } else {
            // client result ..no cleanup needed
            return "{\"result\":\"activities completed first..." + input.getTimer() + "\"}";
        }
    }

    private void runActivities() {
        try {
            activities.one();
            activities.two();
            activities.three();
            activities.four(); // needs cancellation only
            // TODO -- which activities are in cancelation scope
            // should be easy to configure

        } catch (ActivityFailure af) {
            // call activity 4 again
            // TODO - what do here....
        }
    }

}
