package io.temporal.sample.comp;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowServiceException;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class SampleSaga {
    private final Options options;
    private final List<CompensationInfo> compensationInfo = new ArrayList<>();
    private final Logger logger;
    private final SampleInput input;

    public static final class CompensationInfo {
        private String activityType;
        private String taskQueue;
        private Object input;

        public CompensationInfo() {
        }

        private CompensationInfo(String activityType, String taskQueue,
                                 Object input) {
            this.activityType = activityType;
            this.taskQueue = taskQueue;
            this.input = input;
        }

        public static final class Builder {
            private String activityType;
            private String taskQueue = "sagacleanupqueue";
            private Object input;

            public CompensationInfo.Builder setActivityType(String activityType) {
                this.activityType = activityType;
                return this;
            }

            public CompensationInfo.Builder setTaskQueue(String taskQueue) {
                this.taskQueue = taskQueue;
                return this;
            }

            public CompensationInfo.Builder setInput(Object input) {
                this.input = input;
                return this;
            }

            public CompensationInfo build() {
                return new CompensationInfo(activityType, taskQueue, input);
            }

        }

    }

    public static final class Options {
        private boolean continueWithError;
        private boolean useParentWorkflowIdAsPrefix;
        private String childId;

        public Options() {
        }

        private Options(boolean continueWithError,
                        boolean useParentWorkflowIdAsPrefix, String childId) {
            this.continueWithError = continueWithError;
            this.useParentWorkflowIdAsPrefix = useParentWorkflowIdAsPrefix;
            this.childId = childId;
        }

        public static final class Builder {
            private boolean continueWithError;
            private boolean useParentWorkflowIdAsPrefix = true;
            private String childId;

            public Options.Builder setContinueWithError(boolean continueWithError) {
                this.continueWithError = continueWithError;
                return this;
            }

            /**
             * useParentWorkflowIdAsPrefix sets the id of the compensation child workflow to use the parent workflow id
             * as prefix. Child workflow id then becomes "parentWorkflowId_childId" if childId is set, otherwise
             * "parentWorkflowId_uuid" is used.
             *
             * @param useParentWorkflowIdAsPrefix
             * @return options builder
             */
            public Options.Builder setUseParentWorkflowIdAsPrefix(boolean useParentWorkflowIdAsPrefix) {
                this.useParentWorkflowIdAsPrefix = useParentWorkflowIdAsPrefix;
                return this;
            }

            /**
             * childId sets the child id to be used, if not set or empty code will not set a workflow id on the Saga
             * child allowing temporal service to assign unique workflow id for it.
             * Note if childId is not set then useParentWorkflowIdAsPrefix is ignored.
             *
             * @param childId
             * @return options builder
             */
            public Options.Builder setChildId(String childId) {
                this.childId = childId;
                return this;
            }

            public Options build() {
                return new Options(continueWithError, useParentWorkflowIdAsPrefix, childId);
            }
        }
    }

    public SampleSaga(Options options, SampleInput input, Logger logger) {
        this.options = options;
        this.input = input;
        this.logger = logger;
    }

    public void addCompensation(CompensationInfo compInfo) {
        compensationInfo.add(compInfo);
    }


    public void compensate() {
        compensate(null);
    }

    public void compensate(CompensationInfo compensateAlwaysInfo) {
        if (compensateAlwaysInfo != null) {
            addCompensation(compensateAlwaysInfo);
        }
        ChildWorkflowOptions.Builder sagaChildOptionsBuilder = ChildWorkflowOptions.newBuilder();
        sagaChildOptionsBuilder.setTaskQueue("sagacleanupqueue");
        if (options.childId != null && options.childId.length() > 0) {
            if (options.useParentWorkflowIdAsPrefix) {
                sagaChildOptionsBuilder.setWorkflowId(Workflow.getInfo().getWorkflowId() + "_" + options.childId);
            } else {
                sagaChildOptionsBuilder.setWorkflowId(options.childId);
            }
        }
        sagaChildOptionsBuilder.setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON);

        SagaChildWorkflow childWorkflow = Workflow.newChildWorkflowStub(
                SagaChildWorkflow.class, sagaChildOptionsBuilder.build()
        );

        try {
            // start child async, wait until it starts (not completes)
            Async.procedure(childWorkflow::cleanup, input, options, compensationInfo);
            Workflow.getWorkflowExecution(childWorkflow).get();
        } catch (ChildWorkflowFailure e) {
            if (e.getCause() instanceof WorkflowExecutionAlreadyStarted) {
                // one reason could be workflow id reuse policy
                logger.error("Child workflow execution already started: " + e.getCause().getMessage());
            } else if (e.getCause() instanceof WorkflowServiceException) {
                // could be some other type of service exception
                logger.error("Service exception starting child: " + e.getCause().getMessage());
            } else {
                // something else?
                logger.error("Exception starting child: " + e.getCause().getMessage());
            }
        }
    }

    @WorkflowInterface
    public interface SagaChildWorkflow {
        @WorkflowMethod
        void cleanup(SampleInput input, Options options, List<CompensationInfo> compensationInfo);
    }

    @WorkflowImpl(taskQueues = "sagacleanupqueue")
    public static class SagaChildWorkflowImpl implements SagaChildWorkflow {

        private Logger logger = Workflow.getLogger(SagaChildWorkflowImpl.class);

        @Override
        public void cleanup(SampleInput input, Options options, List<CompensationInfo> compensationInfo) {
            if (compensationInfo != null) {
                // iterate backwards...
                ListIterator listIterator = compensationInfo.listIterator(compensationInfo.size());
                while (listIterator.hasPrevious()) {
                    CompensationInfo info = (CompensationInfo) listIterator.previous();

                    try {
                        // TODO - figure out how to pass in activity options
                        // for untyped stub its not going to be enhanced with WorkflowImplementationOptions overrides
                        ActivityStub activityStub = Workflow.newUntypedActivityStub(ActivityOptions.newBuilder()
                                // TODO - this should be configurable..for now hard-code
                                .setStartToCloseTimeout(Duration.ofSeconds(2))
                                .setTaskQueue(info.taskQueue)
                                .build());
                        if (info.input != null) {
                            activityStub.execute(info.activityType, SampleResult.class, info.input);
                        } else {
                            activityStub.execute(info.activityType, SampleResult.class, input);
                        }
                    } catch (ActivityFailure af) {
                        if (!options.continueWithError) {
                            throw af;
                        } else {
                            logger.warn("Compensation activity " + info.activityType + " failure: " + af.getMessage());
                        }
                    }
                }
            }
            return;
        }
    }
}
