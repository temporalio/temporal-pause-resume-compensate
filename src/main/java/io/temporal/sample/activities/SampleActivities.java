package io.temporal.sample.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SampleActivities {
    String one();
    String two();
    String three();
    String four();
    String five();
}
