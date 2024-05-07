package io.temporal.sample.workflows;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.sample.activities.SampleActivities;
import io.temporal.sample.model.SampleResult;
import io.temporal.sample.model.WorkflowContext;
import io.temporal.sample.model.dsl.Flow;
import io.temporal.sample.model.dsl.FlowAction;
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
    public SampleResult run(Flow flow) {
        // Saga
        saga = new io.temporal.sample.comp.Saga("cleanupsagachild", "sagacleanupqueue");
        if(flow == null || flow.getActions().isEmpty()) {
            throw ApplicationFailure.newFailure("Flow is null or does not have any actions", "illegal flow");
        }

        try {
            runActions(flow);
        } catch (ActivityFailure e) {
            logger.warn("Activity failure: " + e.getMessage());
            saga.compensate();
            throw ApplicationFailure.newFailure("failing execution after compensation initiated", e.getCause().getClass().getName());
        }
        return new SampleResult("Parent wf: result, no compensation initiated....");
    }

    private void runActions(Flow flow) {

        for(FlowAction action: flow.getActions()) {
            // build activity options based on flow action input
            ActivityOptions.Builder activityOptionsBuilder = ActivityOptions.newBuilder();
            activityOptionsBuilder.setStartToCloseTimeout(Duration.ofSeconds(action.getStartToCloseSec()));
            if(action.getRetries() > 0) {
                activityOptionsBuilder.setRetryOptions(RetryOptions.newBuilder()
                                .setMaximumAttempts(action.getRetries())
                        .build());
            }
            // create untyped activity stub and run activity based on flow action
            ActivityStub activityStub = Workflow.newUntypedActivityStub(activityOptionsBuilder.build());

            // if action has compensation, add it now
            if(action.getCompensateBy() != null && !action.getCompensateBy().isEmpty()) {
                saga.addCompensation(action.getCompensateBy(), new WorkflowContext(action.getCompensateBy()));
            }
            // note for sample simplicity our activities dont have a return type atm
            activityStub.execute(action.getAction(), Void.class);
        }
    }

}
