package io.temporal.sample.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;

@ActivityInterface
public interface SampleCompensationActivities {
    SampleResult compensateOne(SampleInput input);
    SampleResult compensateTwo(SampleInput input);
    SampleResult compensateThree(SampleInput input);
    SampleResult compensateFour(SampleInput input);
    SampleResult compensateAlways(SampleInput input);
}
