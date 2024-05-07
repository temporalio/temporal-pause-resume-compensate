package io.temporal.sample.activities;

import io.temporal.failure.ApplicationFailure;
import io.temporal.sample.model.SampleResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;
import java.util.Random;

import java.util.concurrent.TimeUnit;

@Component
@ActivityImpl(taskQueues = "samplequeue")
public class SampleActivitiesImpl implements SampleActivities {
    @Override
    public SampleResult one() {
        sleep(1);
        throwRandomFailure("activity four");
        return new SampleResult("Activity one done...");
    }

    @Override
    public SampleResult two() {
        sleep(1);
        throwRandomFailure("activity four");
        return new SampleResult("Activity two done...");
    }

    @Override
    public SampleResult three() {
        sleep(1);
        throwRandomFailure("activity four");
        return new SampleResult("Activity three done...");
    }

    @Override
    public SampleResult four() {
//        ActivityExecutionContext context = Activity.getExecutionContext();
//        for (int i = 0; i < 6; i++) {
//            sleep(1);
//            try {
//                // Perform the heartbeat. Used to notify the workflow that activity execution is alive
//                context.heartbeat(i);
//            } catch (ActivityCompletionException e) {
//                throw e;
//            }
//        }
        sleep(1);
        throwRandomFailure("activity four");
        return new SampleResult("Activity four done...");
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
