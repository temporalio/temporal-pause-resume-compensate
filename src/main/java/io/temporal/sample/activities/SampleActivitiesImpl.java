package io.temporal.sample.activities;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "samplequeue")
public class SampleActivitiesImpl implements SampleActivities {
    @Override
    public String one() {
        return null;
    }

    @Override
    public String two() {
        return null;
    }

    @Override
    public String three() {
        return null;
    }

    @Override
    public String four() {
        return null;
    }
}
