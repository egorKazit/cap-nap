package com.yk.nap.service.oauth;

import com.sap.cloud.security.client.HttpClientFactory;
import com.sap.cloud.security.config.CredentialType;
import com.sap.cloud.security.config.OAuth2ServiceConfiguration;
import com.sap.cloud.security.config.OAuth2ServiceConfigurationBuilder;
import com.sap.cloud.security.xsuaa.client.DefaultOAuth2TokenService;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenResponse;
import com.sap.cloud.security.xsuaa.client.OAuth2TokenService;
import com.sap.cloud.security.xsuaa.client.XsuaaDefaultEndpoints;
import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.sap.cloud.security.xsuaa.tokenflows.XsuaaTokenFlows;
import com.yk.nap.utils.HttpOAuthTokenKey;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@Scope("prototype")
public class OAuthTokenClient implements OAuthToken {


    private static final String TENANT_ID = "tenantId";
    private OAuth2TokenResponse oAuth2TokenResponse;

    @Override
    public String fetch(HttpOAuthTokenKey httpOAuthTokenKey) throws TokenFlowException {
        OAuth2ServiceConfiguration oAuth2ServiceConfiguration = getConfig(httpOAuthTokenKey);
        OAuth2TokenService oAuth2TokenService = new DefaultOAuth2TokenService(HttpClientFactory.create(oAuth2ServiceConfiguration.getClientIdentity()));
        var passwordTokenFlowBuilder = new XsuaaTokenFlows(
                oAuth2TokenService,
                new XsuaaDefaultEndpoints(oAuth2ServiceConfiguration),
                oAuth2ServiceConfiguration.getClientIdentity());
        if (httpOAuthTokenKey.username != null && httpOAuthTokenKey.password != null) {
            var passwordTokenFlow = passwordTokenFlowBuilder.passwordTokenFlow();
            passwordTokenFlow.username(httpOAuthTokenKey.username).password(httpOAuthTokenKey.password);
            oAuth2TokenResponse = passwordTokenFlow.execute();
        } else {
            oAuth2TokenResponse = passwordTokenFlowBuilder.clientCredentialsTokenFlow().execute();
        }

        return StringUtils.capitalize(oAuth2TokenResponse.getTokenType() + " " + oAuth2TokenResponse.getAccessToken());

    }

    private OAuth2ServiceConfiguration getConfig(@NonNull HttpOAuthTokenKey httpOAuthTokenKey) {
        return OAuth2ServiceConfigurationBuilder
                .forService(com.sap.cloud.security.config.Service.XSUAA)
                .withUrl(httpOAuthTokenKey.url)
                .withClientId(httpOAuthTokenKey.uaa.clientId)
                .withCredentialType(Optional.ofNullable(CredentialType.from(httpOAuthTokenKey.uaa.credentialType)).orElse(CredentialType.INSTANCE_SECRET))
                .withClientSecret(Optional.ofNullable(httpOAuthTokenKey.uaa.clientSecret).orElse(""))
                .withCertUrl(Optional.ofNullable(httpOAuthTokenKey.uaa.certUrl).orElse(""))
                .withCertificate(Optional.ofNullable(httpOAuthTokenKey.uaa.certificate).orElse(""))
                .withPrivateKey(Optional.ofNullable(httpOAuthTokenKey.uaa.key).orElse(""))
                .withProperty(TENANT_ID, httpOAuthTokenKey.uaa.tenantId).build();
    }

}
