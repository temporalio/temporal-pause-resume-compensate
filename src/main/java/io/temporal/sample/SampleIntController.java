package io.temporal.sample;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.sample.workflows.SampleWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SampleIntController {

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
    ResponseEntity sampleInt() {
        SampleWorkflow workflow =
                client.newWorkflowStub(SampleWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue("samplequeue")
                                .setWorkflowId("sample-workflow")
                                .build());
        return new ResponseEntity<>(workflow.run(), HttpStatus.OK);
    }

}

