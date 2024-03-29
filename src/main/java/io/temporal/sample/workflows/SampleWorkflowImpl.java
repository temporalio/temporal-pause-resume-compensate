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
                    .setStartToCloseTimeout(Duration.ofSeconds(4))
                    .build());

    @Override
    public String run(SampleInput input) {

        // Create the timer promise
        Promise<Void> timerPromise = Workflow.newTimer(Duration.ofSeconds(input.getTimer()));
        // Create activities promise
        Promise<Void> activitiesPromise = Async.procedure(this::runActivities);

        Promise.anyOf(timerPromise, activitiesPromise).get();
        if(timerPromise.isCompleted()) {
            // need to cancel activities here and compensate...
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
