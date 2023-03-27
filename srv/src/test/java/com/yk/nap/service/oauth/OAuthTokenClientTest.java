package com.yk.nap.service.oauth;

import com.google.gson.Gson;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.tokenflows.ClientCredentialsTokenFlow;
import com.sap.cloud.security.xsuaa.tokenflows.PasswordTokenFlow;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;
import com.yk.nap.utils.HttpOAuthTokenKey;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
public class OAuthTokenClientTest {



    @Test
    public void processPasswordOAuth() throws IOException {
        PasswordTokenFlow passwordTokenFlow = mock(PasswordTokenFlow.class);
        OAuth2TokenResponse oAuth2TokenResponse = mock(OAuth2TokenResponse.class);
        when(oAuth2TokenResponse.getTokenType()).thenReturn("TestType");
        when(oAuth2TokenResponse.getAccessToken()).thenReturn("TestTokenPassword");
        when(passwordTokenFlow.execute()).thenReturn(oAuth2TokenResponse);
        when(passwordTokenFlow.username(anyString())).thenReturn(passwordTokenFlow);
        when(passwordTokenFlow.password(anyString())).thenReturn(passwordTokenFlow);
        HttpOAuthTokenKey httpOAuthTokenKey =
                new Gson().fromJson(String.join("",Files.readAllLines(Path.of("src/test/resources/test-workflow-destination.json"))),HttpOAuthTokenKey.class);
        try (MockedConstruction<XsuaaTokenFlows> ignored = mockConstruction(XsuaaTokenFlows.class, (xsuaaTokenFlows, context) -> {
            when(xsuaaTokenFlows.passwordTokenFlow()).thenReturn(passwordTokenFlow);
        })) {
            OAuthTokenClient oAuthTokenClient = new OAuthTokenClient();
            var token = oAuthTokenClient.fetch(httpOAuthTokenKey);
            assertEquals("TestType TestTokenPassword", token);
        }
    }

    @Test
    public void processCredentialsOAuth() throws IOException {
        ClientCredentialsTokenFlow clientCredentialsTokenFlow = mock(ClientCredentialsTokenFlow.class);
        OAuth2TokenResponse oAuth2TokenResponse = mock(OAuth2TokenResponse.class);
        when(oAuth2TokenResponse.getTokenType()).thenReturn("TestType");
        when(oAuth2TokenResponse.getAccessToken()).thenReturn("TestTokenCredentials");
        when(clientCredentialsTokenFlow.execute()).thenReturn(oAuth2TokenResponse);
        HttpOAuthTokenKey httpOAuthTokenKey =
                new Gson().fromJson(String.join("",Files.readAllLines(Path.of("src/test/resources/test-workflow-destination-creds.json"))),HttpOAuthTokenKey.class);
        try (MockedConstruction<XsuaaTokenFlows> ignored = mockConstruction(XsuaaTokenFlows.class, (xsuaaTokenFlows, context) -> {
            when(xsuaaTokenFlows.clientCredentialsTokenFlow()).thenReturn(clientCredentialsTokenFlow);
        })) {
            OAuthTokenClient oAuthTokenClient = new OAuthTokenClient();
            var token = oAuthTokenClient.fetch(httpOAuthTokenKey);
            assertEquals("TestType TestTokenCredentials", token);
        }
    }

}