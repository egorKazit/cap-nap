package com.yk.nap.service.oauth;

import com.sap.cloud.security.xsuaa.tokenflows.TokenFlowException;
import com.yk.nap.utils.HttpOAuthTokenKey;

public interface OAuthToken {

    String fetch(HttpOAuthTokenKey httpOAuthTokenKey) throws TokenFlowException;

}
