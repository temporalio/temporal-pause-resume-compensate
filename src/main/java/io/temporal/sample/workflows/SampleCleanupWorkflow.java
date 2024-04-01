package io.temporal.sample.workflows;

import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SampleCleanupWorkflow {
    @WorkflowMethod
    SampleResult cleanup(SampleInput input);
}
