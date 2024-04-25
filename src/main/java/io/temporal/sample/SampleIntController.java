package io.temporal.sample;

import io.temporal.api.batch.v1.BatchOperationSignal;
import io.temporal.api.workflowservice.v1.StartBatchOperationRequest;
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

import java.util.UUID;

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

        client.getWorkflowServiceStubs()
                .blockingStub()
                .startBatchOperation(
                        StartBatchOperationRequest.newBuilder()
                                .setNamespace(client.getOptions().getNamespace())
                                .setJobId(UUID.randomUUID().toString())
                                .setVisibilityQuery("pause=true")
                                .setReason("Retrying all paused executions")
                                .setSignalOperation(BatchOperationSignal.newBuilder()
                                        .setSignal("retry")
                                        .setIdentity(client.getOptions().getIdentity())
                                        .build())
                                .build());

        return new ResponseEntity<>(new SampleResult("Sent retry signal to all paused workflows"), HttpStatus.OK);
    }

    @PostMapping(value = "/failall")
    ResponseEntity failall() {
        client.getWorkflowServiceStubs()
                .blockingStub()
                .startBatchOperation(
                        StartBatchOperationRequest.newBuilder()
                                .setNamespace(client.getOptions().getNamespace())
                                .setJobId(UUID.randomUUID().toString())
                                .setVisibilityQuery("pause=true")
                                .setReason("Failing all paused executions")
                                .setSignalOperation(BatchOperationSignal.newBuilder()
                                        .setSignal("fail")
                                        .setIdentity(client.getOptions().getIdentity())
                                        .build())
                                .build());

        return new ResponseEntity<>(new SampleResult("Sent fail signal to all paused workflows"), HttpStatus.OK);
    }
}

