package io.temporal.sample.workflows;

import io.temporal.spring.boot.WorkflowImpl;

@WorkflowImpl(taskQueues = "samplequeue")
public class SampleWorkflowImpl implements SampleWorkflow {
    @Override
    public String run() {
        return "{\"result\":\"done...\"}";
    }
}
