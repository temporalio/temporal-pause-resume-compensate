package io.temporal.sample.model;

public class WorkflowContext {
    private String value;

    public WorkflowContext(){}

    public WorkflowContext(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
