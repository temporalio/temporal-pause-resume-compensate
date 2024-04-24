package io.temporal.sample.interceptors;

import io.temporal.workflow.SignalMethod;

public interface PauseResumeInterceptorListener {
    @SignalMethod
    void complete();

    @SignalMethod
    void resume();
}
