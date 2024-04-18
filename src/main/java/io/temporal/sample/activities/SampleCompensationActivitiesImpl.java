package io.temporal.sample.activities;

import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.sample.model.WorkflowContext;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ActivityImpl(taskQueues = "sagacleanupqueue")
public class SampleCompensationActivitiesImpl implements SampleCompensationActivities {
    @Override
    public WorkflowContext compensateOne(WorkflowContext input) {
        sleep(1);
       return new WorkflowContext("compensateOneDone");
    }

    @Override
    public WorkflowContext compensateTwo(WorkflowContext input) {
        sleep(1);
        return new WorkflowContext("compensateTwoDone");
    }

    @Override
    public WorkflowContext compensateThree(WorkflowContext input) {
        sleep(1);
        return new WorkflowContext("compensateThreeDone");
    }

    @Override
    public WorkflowContext compensateFour(WorkflowContext input) {
        sleep(1);
        return new WorkflowContext("compensateFourDone");
    }

    @Override
    public WorkflowContext compensateAlways(WorkflowContext input) {
        sleep(1);
        return new WorkflowContext("compensateFiveDone");
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException ee) {
            // Empty
        }
    }
}
