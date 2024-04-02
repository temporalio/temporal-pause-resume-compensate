package io.temporal.sample.model;

public class SampleResult {
    private String message;

    public SampleResult() {
    }

    public SampleResult(String message) {
        this.message = message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
