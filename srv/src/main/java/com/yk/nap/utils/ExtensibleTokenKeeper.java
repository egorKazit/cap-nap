package com.yk.nap.utils;

import com.yk.nap.service.oauth.OAuthToken;

import javax.annotation.PostConstruct;
import java.io.IOException;

public abstract class ExtensibleTokenKeeper {

    protected final OAuthToken oAuthToken;

    protected ExtensibleTokenKeeper(OAuthToken oAuthToken) {
        this.oAuthToken = oAuthToken;
    }

    @PostConstruct
    protected String getToken() throws IOException {
        HttpOAuthTokenKey httpOAuthTokenKey = getHttpOAuthTokenKey();
        return oAuthToken.fetch(httpOAuthTokenKey);
    }

    protected abstract HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException;

}
