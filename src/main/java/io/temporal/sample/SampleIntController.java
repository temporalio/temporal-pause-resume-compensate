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
        for(int i=0;i < 10; i++) {
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

    @PostMapping(value = "/retryall")
    ResponseEntity retryall() {
        String jobId = UUID.randomUUID().toString();
        client.getWorkflowServiceStubs()
                .blockingStub()
                .startBatchOperation(
                        StartBatchOperationRequest.newBuilder()
                                .setNamespace(client.getOptions().getNamespace())
                                .setJobId(jobId)
                                .setVisibilityQuery("pause=true")
                                .setReason("Retrying all paused executions")
                                .setSignalOperation(BatchOperationSignal.newBuilder()
                                        .setSignal("retry")
                                        .setIdentity(client.getOptions().getIdentity())
                                        .build())
                                .build());

        return new ResponseEntity<>(new SampleResult("Started batch operation to retry all paused executions: " + jobId), HttpStatus.OK);
    }

    @PostMapping(value = "/failall")
    ResponseEntity failall() {
        String jobId = UUID.randomUUID().toString();
        client.getWorkflowServiceStubs()
                .blockingStub()
                .startBatchOperation(
                        StartBatchOperationRequest.newBuilder()
                                .setNamespace(client.getOptions().getNamespace())
                                .setJobId(jobId)
                                .setVisibilityQuery("pause=true")
                                .setReason("Failing all paused executions")
                                .setSignalOperation(BatchOperationSignal.newBuilder()
                                        .setSignal("fail")
                                        .setIdentity(client.getOptions().getIdentity())
                                        .build())
                                .build());

        return new ResponseEntity<>(new SampleResult("Started batch operation to fail all paused executions: " + jobId), HttpStatus.OK);
    }

    @PostMapping(value="/listbatchoperations")
    ResponseEntity listBatches() {
        List<BatchOperationInfo> info =  getBatchInfo(client, null, null);
        if(info != null) {
            String result = Joiner.on(",").join(info.stream()
                    .map( in -> in.getJobId() )
                    .collect( Collectors.toList()));

            return new ResponseEntity<>(new SampleResult("Batch operation ids: " + result), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new SampleResult("Not batch operations running"), HttpStatus.OK);
        }
    }

    @PostMapping(value="/stopbatchoperations")
    ResponseEntity stopbatches() {
        List<BatchOperationInfo> info =  getBatchInfo(client, null, null);
        if(info != null) {
            for(BatchOperationInfo in : info) {
                client.getWorkflowServiceStubs().blockingStub().stopBatchOperation(
                        StopBatchOperationRequest.newBuilder()
                                .setNamespace(client.getOptions().getNamespace())
                                .setReason("stopping batch")
                                .setIdentity(client.getOptions().getIdentity())
                                .setJobId(in.getJobId())
                                .build()
                );
            }
            return new ResponseEntity<>(new SampleResult("Stopped all batches"), HttpStatus.OK);
        }
        return new ResponseEntity<>(new SampleResult("No batches running, nothing to stop"), HttpStatus.OK);
    }

    private List<BatchOperationInfo> getBatchInfo(WorkflowClient client, ByteString nextPageToken, List<BatchOperationInfo> info) {
        if(info == null) {
            info = new ArrayList<>();
        }
        ListBatchOperationsResponse res = client.getWorkflowServiceStubs().blockingStub().listBatchOperations(ListBatchOperationsRequest.newBuilder()
                .setNamespace(client.getOptions().getNamespace())
                .build());
        info.addAll(res.getOperationInfoList());
        if(res.getNextPageToken() == null) {
            return info;
        } else {
            return getBatchInfo(client, res.getNextPageToken(), info);
        }
    }

}

