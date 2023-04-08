package com.yk.nap.service.workflow;

import com.yk.nap.configuration.ParameterHolder;
import com.yk.nap.service.oauth.OAuthToken;
import lombok.NonNull;
import org.apache.http.HttpHeaders;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
public class BTPWorkflowOperatorTest {

    @InjectMocks
    private BTPWorkflowOperator workflowOperator;

    @Mock
    ParameterHolder parameterHolder;

    @Mock
    OAuthToken oAuthToken;

    @Mock
    ApplicationContext applicationContext;

    @Mock
    HttpResponse<String> mockResponse;

    @Mock
    HttpClient httpClient;

    private Supplier<String> getMethod;

    @Before
    public void prepare() throws IOException, InterruptedException {
        when(parameterHolder.getWorkflowUrl()).thenReturn("http://localhost:8082");
        when(parameterHolder.getWorkflowCredentials()).thenReturn("src/test/resources/test-workflow-destination.json");
        when(oAuthToken.fetch(any())).thenReturn("test_oauth");
        when(applicationContext.getBean(OAuthToken.class)).thenReturn(oAuthToken);
        when(httpClient.send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()))).thenAnswer(invocationOnMock -> {
            HttpRequest httpRequest = invocationOnMock.getArgument(0);
            assertEquals(httpRequest.method(), getMethod.get());
            checkForHeaderValueInRequest(httpRequest, HttpHeaders.AUTHORIZATION, "test_oauth");
            checkForHeaderValueInRequest(httpRequest, HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            assertTrue(httpRequest.bodyPublisher().isPresent());
            return mockResponse;
        });
    }

    @Test
    public void startWorkflow() throws IOException, InterruptedException {
        getMethod = HttpMethod.POST::toString;
        when(mockResponse.body()).thenReturn("{\"id\":\"1\"}");
        try (MockedStatic<HttpClient> httpClientMockedStatic = Mockito.mockStatic(HttpClient.class)) {
            httpClientMockedStatic.when(HttpClient::newHttpClient)
                    .thenReturn(httpClient);
            var startResult = workflowOperator.startWorkflow("", new JSONObject());
            verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()));
            assertEquals(startResult.getId(), "1");
        }
    }

    @Test
    public void terminateWorkflow() throws IOException, InterruptedException {
        getMethod = HttpMethod.PATCH::toString;
        when(mockResponse.statusCode()).thenReturn(200);
        try (MockedStatic<HttpClient> httpClientMockedStatic = Mockito.mockStatic(HttpClient.class)) {
            httpClientMockedStatic.when(HttpClient::newHttpClient)
                    .thenReturn(httpClient);
            workflowOperator.terminateWorkflow("");
            verify(httpClient, times(1)).send(any(), any(HttpResponse.BodyHandlers.ofString().getClass()));
        }
    }

    private void checkForHeaderValueInRequest(@NonNull HttpRequest httpRequest, String header, String expectedValue) {
        Optional<String> authorization = httpRequest.headers().firstValue(header);
        assertTrue(authorization.isPresent());
        assertEquals(authorization.get(), expectedValue);
    }

}