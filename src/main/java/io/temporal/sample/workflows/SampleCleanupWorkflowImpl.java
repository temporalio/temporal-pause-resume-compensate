package io.temporal.sample.workflows;

import io.temporal.sample.model.SampleInput;
import io.temporal.spring.boot.WorkflowImpl;

@WorkflowImpl(taskQueues = "samplecleanupqueue")
public class SampleCleanupWorkflowImpl implements SampleCleanupWorkflow {
    @Override
    public String cleanup(SampleInput input) {
        return "cleanup completed...";
    }
}
