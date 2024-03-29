package io.temporal.sample.activities;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@ActivityImpl(taskQueues = "samplequeue")
public class SampleActivitiesImpl implements SampleActivities {
    @Override
    public String one() {
        randSleep();
        return "done...";
    }

    @Override
    public String two() {
        randSleep();
        return "done...";
    }

    @Override
    public String three() {
        randSleep();
        return "done...";
    }

    @Override
    public String four() {
        randSleep();
        return "done...";
    }

    private void randSleep() {
        int min = 1000;
        int max = 4000;
        try {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(min, max));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
