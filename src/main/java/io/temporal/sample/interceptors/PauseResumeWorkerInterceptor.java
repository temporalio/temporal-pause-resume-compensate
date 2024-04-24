package io.temporal.sample.interceptors;

import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkerInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import org.springframework.stereotype.Component;

@Component
public class PauseResumeWorkerInterceptor implements WorkerInterceptor {
    @Override
    public WorkflowInboundCallsInterceptor interceptWorkflow(WorkflowInboundCallsInterceptor next) {
        return new PauseResumeWorkflowInboundCallsInterceptor(next);
    }

    @Override
    public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
        return next;
    }
}
