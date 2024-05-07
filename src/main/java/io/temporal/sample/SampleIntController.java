package io.temporal.sample;

import com.google.common.base.Joiner;
import com.google.protobuf.ByteString;
import io.temporal.api.batch.v1.BatchOperationInfo;
import io.temporal.api.batch.v1.BatchOperationSignal;
import io.temporal.api.workflowservice.v1.ListBatchOperationsRequest;
import io.temporal.api.workflowservice.v1.ListBatchOperationsResponse;
import io.temporal.api.workflowservice.v1.StartBatchOperationRequest;
import io.temporal.api.workflowservice.v1.StopBatchOperationRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.SearchAttributeKey;
import io.temporal.common.SearchAttributes;
import io.temporal.sample.model.SampleInput;
import io.temporal.sample.model.SampleResult;
import io.temporal.sample.workflows.SampleWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class SampleIntController {
    static final SearchAttributeKey<Boolean> PAUSE_SA =
            SearchAttributeKey.forBoolean("pause");

    @Autowired
    WorkflowClient client;

    @GetMapping("/")
    public String update() {
        return "index";
    }

    @PostMapping(
            value = "/run",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity sampleInt(@RequestBody SampleInput input) {
        // start 10 workflows
        for (int i = 0; i < 10; i++) {
            SampleWorkflow workflow =
                    client.newWorkflowStub(SampleWorkflow.class,
                            WorkflowOptions.newBuilder()
                                    .setTaskQueue("samplequeue")
                                    .setWorkflowId("sample-workflow-" + i)
                                    .setTypedSearchAttributes(SearchAttributes.newBuilder()
                                            .set(PAUSE_SA, false)
                                            .build())
                                    .build());
            // start async
            WorkflowClient.start(workflow::run, input);
        }
        try {
            return new ResponseEntity<>(new SampleResult("Started all workflows"), HttpStatus.OK);
        } catch (WorkflowFailedException e) {
            return new ResponseEntity<>(new SampleResult("workflow failed: " + e.getMessage()), HttpStatus.OK);
        }
    }

    @PostMapping(value = "/retrysample")
    ResponseEntity retrySample() {
        return runBatchSignal("WorkflowType='SampleWorkflow' AND pause=true", "retry");
    }

    @PostMapping(value = "/retrycompensation")
    ResponseEntity retryCompensation() {
        return runBatchSignal("WorkflowType='SagaChildWorkflow' AND pause=true", "retry");
    }

    @PostMapping(value = "/failsample")
    ResponseEntity failSample() {
        return runBatchSignal("WorkflowType='SampleWorkflow' AND pause=true", "fail");
    }

    @PostMapping(value = "/failcompensation")
    ResponseEntity failCompensation() {
        return runBatchSignal("WorkflowType='SagaChildWorkflow' AND pause=true", "fail");
    }

    @PostMapping(value = "/listbatchoperations")
    ResponseEntity listBatches() {
        List<BatchOperationInfo> info = getBatchInfo(client, null, null);
        String result = "";
        if (info != null && info.size() > 0) {
            for(BatchOperationInfo in : info) {
                result += "Status: " + in.getState() + " - ID: " + in.getJobId() + "\n\n";
            }
            return new ResponseEntity<>(new SampleResult("Batch operation ids: " + result), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new SampleResult("Not batch operations running"), HttpStatus.OK);
        }
    }

    @PostMapping(value = "/stopbatchoperations")
    ResponseEntity stopbatches() {
        List<BatchOperationInfo> info = getBatchInfo(client, null, null);
        if (info != null) {
            for (BatchOperationInfo in : info) {
                try {
                    client.getWorkflowServiceStubs().blockingStub().stopBatchOperation(
                            StopBatchOperationRequest.newBuilder()
                                    .setNamespace(client.getOptions().getNamespace())
                                    .setReason("stopping batch")
                                    .setIdentity(client.getOptions().getIdentity())
                                    .setJobId(in.getJobId())
                                    .build()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return new ResponseEntity<>(new SampleResult("Stopped all batches"), HttpStatus.OK);
        }
        return new ResponseEntity<>(new SampleResult("No batches running, nothing to stop"), HttpStatus.OK);
    }

    private List<BatchOperationInfo> getBatchInfo(WorkflowClient client, ByteString nextPageToken, List<BatchOperationInfo> info) {
        if (info == null) {
            info = new ArrayList<>();
        }
        ListBatchOperationsResponse res = client.getWorkflowServiceStubs().blockingStub().listBatchOperations(ListBatchOperationsRequest.newBuilder()
                .setNamespace(client.getOptions().getNamespace())
                .build());
        info.addAll(res.getOperationInfoList());
        if (res.getNextPageToken() != null && res.getNextPageToken().size() > 0) {
            return getBatchInfo(client, res.getNextPageToken(), info);
        } else {
            return info;
        }
    }

    private ResponseEntity runBatchSignal(String visibilityQuery, String signalName) {
        String jobId = UUID.randomUUID().toString();
        client.getWorkflowServiceStubs()
                .blockingStub()
                .startBatchOperation(
                        StartBatchOperationRequest.newBuilder()
                                .setNamespace(client.getOptions().getNamespace())
                                .setJobId(jobId)
                                .setVisibilityQuery(visibilityQuery)
                                .setReason("Retrying all paused executions")
                                .setSignalOperation(BatchOperationSignal.newBuilder()
                                        .setSignal(signalName)
                                        .setIdentity(client.getOptions().getIdentity())
                                        .build())
                                .build());

        return new ResponseEntity<>(new SampleResult("Started batch operation to retry all paused executions: " + jobId), HttpStatus.OK);
    }

}

