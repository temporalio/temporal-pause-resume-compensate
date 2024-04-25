package io.temporal.sample.interceptors;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.SignalMethod;

public interface PauseResumeInterceptorListener {
    @SignalMethod
    void retry();

    @SignalMethod
    void fail();

    @QueryMethod
    String getPendingActivitiesStatus();
}
