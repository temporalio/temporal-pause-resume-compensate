package io.temporal.sample.interceptors;

import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;

public class PauseResumeWorkflowInboundCallsInterceptor extends WorkflowInboundCallsInterceptorBase {
    public PauseResumeWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
        super(next);
    }

    @Override
    public void init(WorkflowOutboundCallsInterceptor outboundCalls) {
        super.init(new PauseResumeWorkflowOutboundCallsInterceptor(outboundCalls));
    }
}
