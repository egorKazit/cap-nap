package com.yk.nap.service.workflow;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.json.JSONObject;

import java.io.IOException;

public interface WorkflowOperator {

    WorkflowPresentation startWorkflow(String workflowId, JSONObject context) throws IOException, InterruptedException;

    boolean terminateWorkflow(String instanceId) throws IOException, InterruptedException;

    @Getter
    class WorkflowPresentation{
        private String id;
    }

}
