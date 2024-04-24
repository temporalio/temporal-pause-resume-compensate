package io.temporal.sample.workflows;

import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.sample.activities.SampleActivities;
import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.sample.model.WorkflowContext;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.*;
import org.slf4j.Logger;

import java.time.Duration;

@WorkflowImpl(taskQueues = "samplequeue")
public class SampleWorkflowImpl implements SampleWorkflow {
    private final Logger logger = Workflow.getLogger(SampleWorkflowImpl.class);
    // note per-activity options are set in TemporalOptionsConfig
    private final SampleActivities activities = Workflow.newActivityStub(SampleActivities.class);
    private Promise<Void> activitiesPromise;
    Promise<Void> timerPromise;
    private static io.temporal.sample.comp.Saga saga;

    @Override
    public SampleResult run(SampleInput input) {
        // Saga
        saga = new io.temporal.sample.comp.Saga("cleanupsagachild", "sagacleanupqueue");

        // Create cancellation scope for timer
//        CancellationScope timerCancellationScope =
//                Workflow.newCancellationScope(
//                        () -> {
//                            timerPromise = Workflow.newTimer(Duration.ofSeconds(input.getTimer()));
//                        });
//        timerCancellationScope.run();
//        // Create cancellation scope for activities
//        CancellationScope activityCancellationScope =
//                Workflow.newCancellationScope(
//                        () -> {
//                            activitiesPromise = Async.procedure(this::runActivities);
//                        });
//        activityCancellationScope.run();

        // Wait for timer and activities promises, whichever completes first
        try {
            runActivities();
        } catch (ActivityFailure e) {
            // We need to handler ActivityFailure here as it will be delivered to workflow code in this .get() call
            // However we just log it as will handle later with activitiesPromise.getFailure
            // If we dont handle it here we would fail execution
            logger.warn("Activity failure: " + e.getMessage());
            saga.compensate();
            throw ApplicationFailure.newFailure("failing execution after saga", "Saga...");
        }
        return new SampleResult("Parent wf: result, no compensation initiated....");

//        // if our timer promise completed but activities are still running
//        if (timerPromise.isCompleted() && !activitiesPromise.isCompleted()) {
//            activityCancellationScope.cancel("timer fired");
//            // run compensation in async child wf
//            saga.compensate();
//            // fail execution
//            throw ApplicationFailure.newFailure("failing execution", "TimerFired");
//        } else {
//            // cancel timer so TimerFired does not get delivered to our worker
//            // in case timer does fire before or at the time we want to complete execution
//            timerCancellationScope.cancel("activities completed/failed before timer");
//            if (activitiesPromise.getFailure() != null) {
//                // run compensation in async child wf
//                saga.compensate();
//                return new SampleResult("Parent wf: result, compensation initiated...");
//            }
//            return new SampleResult("Parent wf: result, no compensation initiated....");
//        }
    }

    private void runActivities() {

        saga.addCompensation("CompensateOne", new WorkflowContext("CompensateOne"));
        activities.one();

        saga.addCompensation("CompensateTwo", new WorkflowContext("CompensateTwo"));
        activities.two();

        saga.addCompensation("CompensateThree", new WorkflowContext("CompensateThree"));
        activities.three();

        saga.addCompensation("CompensateFour", new WorkflowContext("CompensateFour"));
        activities.four();
    }

}
