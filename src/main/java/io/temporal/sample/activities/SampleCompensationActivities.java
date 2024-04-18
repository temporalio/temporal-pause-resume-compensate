package io.temporal.sample.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.sample.model.WorkflowContext;

@ActivityInterface
public interface SampleCompensationActivities {
    WorkflowContext compensateOne(WorkflowContext input);
    WorkflowContext compensateTwo(WorkflowContext input);
    WorkflowContext compensateThree(WorkflowContext input);
    WorkflowContext compensateFour(WorkflowContext input);
    WorkflowContext compensateAlways(WorkflowContext input);
}
