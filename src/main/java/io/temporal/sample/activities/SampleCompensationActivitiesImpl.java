package io.temporal.sample.activities;

import io.temporal.failure.ApplicationFailure;
import io.temporal.sample.model.WorkflowContext;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@ActivityImpl(taskQueues = "sagacleanupqueue")
public class SampleCompensationActivitiesImpl implements SampleCompensationActivities {
    @Override
    public WorkflowContext compensateOne(WorkflowContext input) {
        sleep(1);
        throwRandomFailure("compensation one");
       return new WorkflowContext("compensateOneDone");
    }

    @Override
    public WorkflowContext compensateTwo(WorkflowContext input) {
        sleep(1);
        throwRandomFailure("compensation two");
        return new WorkflowContext("compensateTwoDone");
    }

    @Override
    public WorkflowContext compensateThree(WorkflowContext input) {
        sleep(1);
        throwRandomFailure("compensation three");
        return new WorkflowContext("compensateThreeDone");
    }

    @Override
    public WorkflowContext compensateFour(WorkflowContext input) {
        sleep(1);
        throwRandomFailure("compensation four");
        return new WorkflowContext("compensateFourDone");
    }

    @Override
    public WorkflowContext compensateAlways(WorkflowContext input) {
        sleep(1);
        throwRandomFailure("compensation always");
        return new WorkflowContext("compensateAlways");
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException ee) {
            // Empty
        }
    }

    private void throwRandomFailure(String activityName) {
        Random random = new Random();
        double randomValue = random.nextDouble();
        if (randomValue < 0.10) { // 10% chance of failure
            throw ApplicationFailure.newNonRetryableFailure("simulated failure from " + activityName,
                    "some error", null);
        }
    }
}
