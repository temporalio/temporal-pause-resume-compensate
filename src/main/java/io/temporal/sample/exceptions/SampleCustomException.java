package io.temporal.sample.exceptions;

public class SampleCustomException extends Exception {
    public SampleCustomException(String errorMessage) {
        super(errorMessage);
    }
}
