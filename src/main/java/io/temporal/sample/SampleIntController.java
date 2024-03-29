package io.temporal.sample;

import io.temporal.client.WorkflowClient;
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
            produces = {MediaType.TEXT_HTML_VALUE})
    ResponseEntity customizeSample() {
//        CustomizeWorkflow workflow =
//                client.newWorkflowStub(
//                        CustomizeWorkflow.class,
//                        WorkflowOptions.newBuilder()
//                                .setTaskQueue("CustomizeTaskQueue")
//                                .setWorkflowId("CustomizeSample")
//                                .build());
//
//        // bypass thymeleaf, don't return template name just result
        return new ResponseEntity<>("abc...", HttpStatus.OK);
    }

}

