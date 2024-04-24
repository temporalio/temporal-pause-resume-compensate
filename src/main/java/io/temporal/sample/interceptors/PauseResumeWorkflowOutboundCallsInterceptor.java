package io.temporal.sample.interceptors;

import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.Workflow;

public class PauseResumeWorkflowOutboundCallsInterceptor extends WorkflowOutboundCallsInterceptorBase {
    private ActivityRetryState pendingActivity;
    private enum Action {
        RETRY,
        FAIL
    }

    public PauseResumeWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
        super(next);
        Workflow.registerListener(
                new PauseResumeInterceptorListener() {
                    @Override
                    public void resume() {
                        if(pendingActivity != null) {
                            pendingActivity.retry();
                        } else {
//                            System.out.println("***** CANNOT RESUME NOTHING FAILED...");
                        }
                    }

                    @Override
                    public void fail() {
                        if(pendingActivity != null) {
                            pendingActivity.fail();
                        } else {
//                            System.out.println("***** CANNOT FAIL NOTHING FAILED...");
                        }
                    }
                });
    }

    @Override
    public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
        ActivityOutput<R> result = super.executeActivity(input);
        if(result.getResult().getFailure() != null) {
            pendingActivity = new ActivityRetryState<>(input);
            return pendingActivity.execute(result.getActivityId());
        } else {
            return result;
        }
    }

    private class ActivityRetryState<R> {
        private final ActivityInput<R> input;
        private final CompletablePromise<R> asyncResult = Workflow.newPromise();
        private CompletablePromise<Action> action;
        private ActivityOutput<R> result;

        private ActivityRetryState(ActivityInput<R> input) {
            this.input = input;
        }

        ActivityOutput<R> execute(String activityId) {
            action = Workflow.newPromise();
            action.thenApply(
                    a -> {
                        if (a == Action.FAIL) {
                            // simulate failure of activity result
                            CompletablePromise<R> asyncResult = Workflow.newPromise();
                            asyncResult.completeExceptionally(ApplicationFailure.newNonRetryableFailure("Simulated Failure...", "sim failure..."));
                            result =  new ActivityOutput<>(activityId, asyncResult);
                        } else {
                            result =  PauseResumeWorkflowOutboundCallsInterceptor.super.executeActivity(input);
                        }
                        return null;
                    });
            action.get();
            return result;
        }

        public void fail() {
            if (action == null) {
                return;
            }
            action.complete(Action.FAIL);
        }

        public void retry() {
            if (action == null) {
                return;
            }
            action.complete(Action.RETRY);
        }
    }
}
