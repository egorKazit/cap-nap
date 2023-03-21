package com.yk.nap.service.workflow;

import com.google.gson.Gson;
import com.yk.nap.configuration.ParameterHolder;
import com.yk.nap.service.oauth.OAuthToken;
import com.yk.nap.utils.ExtensibleTokenKeeper;
import com.yk.nap.utils.HttpOAuthTokenKey;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
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

    private final ParameterHolder parameterHolder;

    protected BTPWorkflowOperator(OAuthToken oAuthToken, ParameterHolder parameterHolder) {
        super(oAuthToken);
        this.parameterHolder = parameterHolder;
    }

    @Override
    public WorkflowPresentation startWorkflow(String workflowId, JSONObject context) throws IOException, InterruptedException {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(parameterHolder.getWorkflowUrl() + "/v1/workflow-instances"))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject().put("definitionId", workflowId).put("context", context).toString())).build();
        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return new Gson().fromJson(httpResponse.body(), WorkflowPresentation.class);
    }

    @Override
    public boolean terminateWorkflow(String instanceId) throws IOException, InterruptedException {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(parameterHolder.getWorkflowUrl() + "/v1/workflow-instances/" + instanceId))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(new JSONObject()
                        .put("status", "CANCELED")
                        .put("cascade", false).toString())).build();
        HttpResponse<String> httpResponse = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return httpResponse.statusCode() == 200 || httpResponse.statusCode() == 202;
    }

    @Override
    protected HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException {
        return new Gson().fromJson(String.join("", Files.readAllLines(Path.of(parameterHolder.getWorkflowCredentials()))), HttpOAuthTokenKey.class);
    }
}
