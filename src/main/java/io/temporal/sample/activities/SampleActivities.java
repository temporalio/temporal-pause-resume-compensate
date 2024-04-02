package io.temporal.sample.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.sample.model.SampleResult;

@ActivityInterface
public interface SampleActivities {
    SampleResult one();
    SampleResult two();
    SampleResult three();
    SampleResult four();
}
