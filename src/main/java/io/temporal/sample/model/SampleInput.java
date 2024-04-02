package io.temporal.sample.model;

public class SampleInput {
    private int timer;

    public SampleInput() {
    }

    public SampleInput(int timer) {
        this.timer = timer;
    }

    public int getTimer() {
        return timer;
    }

    public void setTimer(int timer) {
        this.timer = timer;
    }
}
