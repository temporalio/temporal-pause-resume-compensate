package io.temporal.sample.workflows;

import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.spring.boot.WorkflowImpl;

@WorkflowImpl(taskQueues = "samplecleanupqueue")
public class SampleCleanupWorkflowImpl implements SampleCleanupWorkflow {
    @Override
    public SampleResult cleanup(SampleInput input) {
        return new SampleResult("Child - cleanup completed...");
    }
}
