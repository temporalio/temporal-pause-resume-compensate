package io.temporal.sample.workflows;

import io.temporal.sample.model.SampleInput;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface SampleWorkflow {
    @WorkflowMethod
    String run(SampleInput input);
}
