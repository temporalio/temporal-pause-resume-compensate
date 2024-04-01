package io.temporal.sample.workflows;

import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowServiceException;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.sample.activities.SampleActivities;
import io.temporal.sample.exceptions.SampleCustomException;
import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.*;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = "samplequeue")
public class SampleWorkflowImpl implements SampleWorkflow {
    private Logger logger = Workflow.getLogger(SampleWorkflowImpl.class);

    // note per-activity options are set in TemporalOptionsConfig
    private SampleActivities activities = Workflow.newActivityStub(SampleActivities.class);
    private Promise<Void> activitiesPromise;

    @Override
    public SampleResult run(SampleInput input) {
        // timer and activities promises
        Promise<Void> timerPromise = Workflow.newTimer(Duration.ofSeconds(input.getTimer()));

        // Create cancellation scope for activities
        CancellationScope scope =
                Workflow.newCancellationScope(
                        () -> {
                            activitiesPromise = Async.procedure(this::runActivities);
                        });
        scope.run();

        // Wait for timer and activities promises, whichever completes first
        try {
            Promise.anyOf(timerPromise, activitiesPromise).get();
        } catch (ActivityFailure e) {
            // We need to handler ActivityFailure here as it will be delivered to workflow code in this .get() call
            // However we just log it as will handle later with activitiesPromise.getFailure
            // If we dont handle it here we would fail execution
            logger.warn("Activity failure: " + e.getMessage());
        }

        // if our timer promise completed but activities are still running
        if (timerPromise.isCompleted() && !activitiesPromise.isCompleted()) {
            scope.cancel("timer fired...");
            SampleCleanupWorkflow cleanupChild =
                    Workflow.newChildWorkflowStub(SampleCleanupWorkflow.class,
                            ChildWorkflowOptions.newBuilder()
                                    // If not set would use parent task queue name
                                    .setTaskQueue("samplecleanupqueue")
                                    .setWorkflowId(Workflow.getInfo().getWorkflowId() + "-cleanup")
                                    .setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON)
                                    .build());

            try {
                // start child async, wait until it starts (not completes)
                Async.function(cleanupChild::cleanup, input);
                Workflow.getWorkflowExecution(cleanupChild).get(); // 200 ms
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

            // fail execution
            throw Workflow.wrap(new SampleCustomException("failing execution..."));
        } else {
            // if any activities failed we want to call our "persist" activity again
            if(activitiesPromise.getFailure() != null) {
                logger.info("Calling our follow-up activity that needs to run before we respond");
                activities.five();
            }
            return new SampleResult("Parent wf: normal result...");
        }
    }

    private void runActivities() {
        try {
            activities.one();
            activities.two();
            activities.three();
            activities.four();
        } catch (ActivityFailure af) {
            throw af;
        }
    }

}
