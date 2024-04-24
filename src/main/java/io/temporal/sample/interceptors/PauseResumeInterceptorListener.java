package io.temporal.sample.interceptors;

import io.temporal.workflow.SignalMethod;

public interface PauseResumeInterceptorListener {
    @SignalMethod
    void fail();

    @SignalMethod
    void resume();
}
