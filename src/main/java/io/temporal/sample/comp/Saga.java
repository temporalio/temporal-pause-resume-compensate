package io.temporal.sample.comp;

import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.ParentClosePolicy;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowServiceException;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ChildWorkflowFailure;
import io.temporal.sample.model.WorkflowContext;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.*;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class Saga {
    private String activityTaskQueue;
    private String childId;
    private boolean useParentWorkflowIdAsPrefix = true;
    private final Map<String, WorkflowContext> compensationInfo = new HashMap<>();
    private final Logger logger = Workflow.getLogger(this.getClass().getName());

    public Saga(String childId, String activityTaskQueue) {
        this.childId = childId;
        this.activityTaskQueue = activityTaskQueue;
    }

    public Saga(String childId, String activityTaskQueue, boolean useParentWorkflowIdAsPrefix) {
        this.childId = childId;
        this.activityTaskQueue = activityTaskQueue;
        this.useParentWorkflowIdAsPrefix = useParentWorkflowIdAsPrefix;
    }

    public void addCompensation(String activityType, WorkflowContext input) {
        compensationInfo.put(activityType, input);
    }


    public void compensate() {
        compensate(null, null);
    }

    public void compensate(String activityType, WorkflowContext input) {
        if (activityType != null && !activityType.isEmpty()) {
            addCompensation(activityType, input);
        }

        if (compensationInfo.isEmpty()) {
            // no need to run compensation if there is nothing to compensate
            return;
        }

        ChildWorkflowOptions.Builder sagaChildOptionsBuilder = ChildWorkflowOptions.newBuilder();
        sagaChildOptionsBuilder.setTaskQueue("sagacleanupqueue");
        if (childId != null && !childId.isEmpty()) {
            if (useParentWorkflowIdAsPrefix) {
                sagaChildOptionsBuilder.setWorkflowId(Workflow.getInfo().getWorkflowId() + "_" + childId);
            } else {
                sagaChildOptionsBuilder.setWorkflowId(childId);
            }
        }
        sagaChildOptionsBuilder.setParentClosePolicy(ParentClosePolicy.PARENT_CLOSE_POLICY_ABANDON);

        SagaChildWorkflow childWorkflow = Workflow.newChildWorkflowStub(
                SagaChildWorkflow.class, sagaChildOptionsBuilder.build()
        );

        try {
            // start child async, wait until it starts (not completes)
            Async.procedure(childWorkflow::cleanup, activityTaskQueue, compensationInfo);
            Promise<WorkflowExecution> childExecution = Workflow.getWorkflowExecution(childWorkflow);
            childExecution.get();

            //Workflow.getWorkflowExecution(childWorkflow).get();
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
        } catch (Exception ex) {
            logger.error("Exception starting child: " + ex.getCause().getMessage());
        }
    }

    @WorkflowInterface
    public interface SagaChildWorkflow {
        @WorkflowMethod
        void cleanup(String activityTaskQueue, Map<String, WorkflowContext> compensationInfo);
    }

    @WorkflowImpl(taskQueues = "sagacleanupqueue")
    public static class SagaChildWorkflowImpl implements SagaChildWorkflow {

        private Logger logger = Workflow.getLogger(this.getClass().getName());

        @Override
        public void cleanup(String activityTaskQueue, Map<String, WorkflowContext> compensationInfo) {
            if (compensationInfo != null && compensationInfo.size() > 0) {

                for (String activityType : compensationInfo.keySet()) {
                    ActivityStub activityStub = Workflow.newUntypedActivityStub(ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(2))
                            .setTaskQueue(activityTaskQueue)
                            .build());

                    try {
                        activityStub.execute(activityType, WorkflowContext.class, compensationInfo.get(activityType));
                    } catch (ActivityFailure af) {
                        logger.error("Compensation activity failure: " + af.getMessage());
                    }


                }
            }
        }
    }
}
