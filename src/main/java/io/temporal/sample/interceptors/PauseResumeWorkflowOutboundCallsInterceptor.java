package io.temporal.sample.interceptors;

import com.google.common.base.Throwables;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowOutboundCallsInterceptorBase;
import io.temporal.workflow.CompletablePromise;
import io.temporal.workflow.Workflow;

import java.util.ArrayList;
import java.util.List;

public class PauseResumeWorkflowOutboundCallsInterceptor extends WorkflowOutboundCallsInterceptorBase {
    private enum Action {
        RETRY,
        FAIL
    }

    private class ActivityRetryState<R> {
        private final ActivityInput<R> input;
        private final CompletablePromise<R> asyncResult = Workflow.newPromise();
        private CompletablePromise<Action> action;
        private Exception lastFailure;
        private int attempt;

        private ActivityRetryState(ActivityInput<R> input) {
            this.input = input;
        }

        ActivityOutput<R> execute() {
            return executeWithAsyncRetry();
        }

        private ActivityOutput<R> executeWithAsyncRetry() {
            attempt++;
            lastFailure = null;
            action = null;
            ActivityOutput<R> result =
                    PauseResumeWorkflowOutboundCallsInterceptor.super.executeActivity(input);
            result
                    .getResult()
                    .handle(
                            (r, failure) -> {
                                // No failure complete
                                if (failure == null) {
                                    pendingActivities.remove(this);
                                    asyncResult.complete(r);
                                    return null;
                                }
                                // Asynchronously executes requested action when signal is received.
                                lastFailure = failure;
                                action = Workflow.newPromise();
                                return action.thenApply(
                                        a -> {
                                            if (a == Action.FAIL) {
                                                asyncResult.completeExceptionally(failure);
                                            } else {
                                                // Retries recursively.
                                                executeWithAsyncRetry();
                                            }
                                            return null;
                                        });
                            });
            return new ActivityOutput<>(result.getActivityId(), asyncResult);
        }

        public void retry() {
            if (action == null) {
                return;
            }
            action.complete(Action.RETRY);
        }

        public void fail() {
            if (action == null) {
                return;
            }
            action.complete(Action.FAIL);
        }

        public String getStatus() {
            String activityName = input.getActivityName();
            if (lastFailure == null) {
                return "Executing activity \"" + activityName + "\". Attempt=" + attempt;
            }
            if (!action.isCompleted()) {
                return "Last \""
                        + activityName
                        + "\" activity failure:\n"
                        + Throwables.getStackTraceAsString(lastFailure)
                        + "\n\nretry or fail ?";
            }
            return (action.get() == Action.RETRY ? "Going to retry" : "Going to fail")
                    + " activity \""
                    + activityName
                    + "\"";
        }
    }

    private final List<ActivityRetryState<?>> pendingActivities = new ArrayList<>();

    public PauseResumeWorkflowOutboundCallsInterceptor(WorkflowOutboundCallsInterceptor next) {
        super(next);
        Workflow.registerListener(
                new PauseResumeInterceptorListener() {
                    @Override
                    public void retry() {
                        for (ActivityRetryState<?> pending : pendingActivities) {
                            pending.retry();
                        }
                    }

                    @Override
                    public void fail() {
                        for (ActivityRetryState<?> pending : pendingActivities) {
                            pending.fail();
                        }
                    }

                    @Override
                    public String getPendingActivitiesStatus() {
                        StringBuilder result = new StringBuilder();
                        for (ActivityRetryState<?> pending : pendingActivities) {
                            if (result.length() > 0) {
                                result.append('\n');
                            }
                            result.append(pending.getStatus());
                        }
                        return result.toString();
                    }
                });
    }

    @Override
    public <R> ActivityOutput<R> executeActivity(ActivityInput<R> input) {
        ActivityRetryState<R> retryState = new ActivityRetryState<R>(input);
        pendingActivities.add(retryState);
        return retryState.execute();
    }
}
