package io.temporal.sample.workflows;

import io.temporal.sample.model.SampleInput;
import io.temporal.spring.boot.WorkflowImpl;

@WorkflowImpl(taskQueues = "samplequeue")
public class SampleWorkflowImpl implements SampleWorkflow {
    @Override
    public String run(SampleInput input) {
        return "{\"result\":\"done..." + input.getTimer() +"\"}";
    }
}
