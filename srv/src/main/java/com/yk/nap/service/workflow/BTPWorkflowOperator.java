package com.yk.nap.service.workflow;

import com.google.api.client.http.HttpMethods;
import com.google.gson.Gson;
import com.yk.nap.configuration.ParameterHolder;
import com.yk.nap.service.oauth.OAuthToken;
import com.yk.nap.utils.ExtensibleTokenKeeper;
import com.yk.nap.utils.HttpOAuthTokenKey;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Qualifier("BTPWorkflowOperator")
@Scope("prototype")
public class BTPWorkflowOperator extends ExtensibleTokenKeeper implements WorkflowOperator {

    public static final String WORKFLOW_INSTANCE_PATH = "/v1/workflow-instances";
    public static final String WORKFLOW_DEFINITION_ID = "definitionId";
    public static final String WORKFLOW_CONTEXT = "context";
    public static final String WORKFLOW_STATUS = "status";
    public static final String WORKFLOW_CASCADE = "cascade";
    public static final String WORKFLOW_STATUS_CANCELED = "CANCELED";

    private final ParameterHolder parameterHolder;

    protected BTPWorkflowOperator(OAuthToken oAuthToken, ParameterHolder parameterHolder) {
        super(oAuthToken);
        this.parameterHolder = parameterHolder;
    }

    @Override
    public WorkflowPresentation startWorkflow(String workflowId, JSONObject context) throws IOException, InterruptedException {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(parameterHolder.getWorkflowUrl() + WORKFLOW_INSTANCE_PATH))
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject()
                        .put(WORKFLOW_DEFINITION_ID, workflowId)
                        .put(WORKFLOW_CONTEXT, context).toString())).build();
        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return new Gson().fromJson(httpResponse.body(), WorkflowPresentation.class);
    }

    @Override
    public boolean terminateWorkflow(String instanceId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(parameterHolder.getWorkflowUrl() + WORKFLOW_INSTANCE_PATH + "/" + instanceId))
                .header(HttpHeaders.AUTHORIZATION, getToken())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .method(HttpMethods.PATCH, HttpRequest.BodyPublishers.ofString(new JSONObject()
                        .put(WORKFLOW_STATUS, WORKFLOW_STATUS_CANCELED)
                        .put(WORKFLOW_CASCADE, false).toString())).build();
        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return httpResponse.statusCode() == 200 || httpResponse.statusCode() == 202;
    }

    @Override
    protected HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException {
        return new Gson().fromJson(String.join("", Files.readAllLines(Path.of(parameterHolder.getWorkflowCredentials()))), HttpOAuthTokenKey.class);
    }
}
