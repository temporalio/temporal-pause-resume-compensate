package io.temporal.sample.activities;

import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ActivityImpl(taskQueues = "sagacleanupqueue")
public class SampleCompensationActivitiesImpl implements SampleCompensationActivities {
    @Override
    public SampleResult compensateOne(SampleInput input) {
        sleep(1);
        return new SampleResult("Compensate activity one done...");
    }

    @Override
    public SampleResult compensateTwo(SampleInput input) {
        sleep(1);
        return new SampleResult("Compensate activity two done...");
    }

    @Override
    public SampleResult compensateThree(SampleInput input) {
        sleep(1);
        return new SampleResult("Compensate activity three done...");
    }

    @Override
    public SampleResult compensateFour(SampleInput input) {
        sleep(1);
        return new SampleResult("Compensate activity four done...");
    }

    @Override
    public SampleResult compensateAlways(SampleInput input) {
        sleep(1);
        return new SampleResult("Compensate compensateAlways one done...");
    }

    private void sleep(int seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException ee) {
            // Empty
        }
    }
}
