/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.sample.signaler;

import com.google.protobuf.ByteString;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsRequest;
import io.temporal.api.workflowservice.v1.ListWorkflowExecutionsResponse;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class ResumePausedWorkflows {
    private static final WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    private static final WorkflowClient client = WorkflowClient.newInstance(service);

    public static void main(String[] args) {
        resumePausedWorkflowExecutions(
                "pause='true' and ExecutionStatus="
                        + WorkflowExecutionStatus.WORKFLOW_EXECUTION_STATUS_RUNNING,
                null);

        // Cleanup
        service.shutdown();
        System.exit(0);
    }

    private static void resumePausedWorkflowExecutions(String query, ByteString token) {

        ListWorkflowExecutionsRequest request;

        if (token == null) {
            request =
                    ListWorkflowExecutionsRequest.newBuilder()
                            .setNamespace(client.getOptions().getNamespace())
                            .setQuery(query)
                            .build();
        } else {
            request =
                    ListWorkflowExecutionsRequest.newBuilder()
                            .setNamespace(client.getOptions().getNamespace())
                            .setQuery(query)
                            .setNextPageToken(token)
                            .build();
        }

        ListWorkflowExecutionsResponse response =
                service.blockingStub().listWorkflowExecutions(request);

        for (WorkflowExecutionInfo workflowExecutionInfo : response.getExecutionsList()) {
            System.out.println(
                    "Workflow ID: "
                            + workflowExecutionInfo.getExecution().getWorkflowId()
                            + " Run ID: "
                            + workflowExecutionInfo.getExecution().getRunId()
                            + " Status: "
                            + workflowExecutionInfo.getStatus());
            if (workflowExecutionInfo.getParentExecution() != null) {
                System.out.println(
                        "****** PARENT: "
                                + workflowExecutionInfo.getExecution().getWorkflowId()
                                + " - "
                                + workflowExecutionInfo.getExecution().getRunId());
                signalWorkflow(workflowExecutionInfo.getExecution().getWorkflowId());
            }
        }

        if (response.getNextPageToken() != null && response.getNextPageToken().size() > 0) {
            resumePausedWorkflowExecutions(query, response.getNextPageToken());
        }
    }

    private static void signalWorkflow(String workflowId) {
        WorkflowStub existingUntyped = client.newUntypedWorkflowStub(workflowId);

        try {
            System.out.println("Sending resume signal to workflowId " +workflowId);
            existingUntyped.signal("resume");
        } catch (Exception e) {
            System.err.println("Failed to signal workflow with ID " + workflowId);
        }
    }
}
