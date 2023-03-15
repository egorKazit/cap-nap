package com.yk.nap.utils;

import com.yk.nap.service.oauth.OAuthToken;
import com.yk.nap.utils.HttpOAuthTokenKey;

import javax.annotation.PostConstruct;
import java.io.IOException;

public abstract class ExtensibleTokenKeeper {

    protected final OAuthToken oAuthToken;
    protected String token;

    protected ExtensibleTokenKeeper(OAuthToken oAuthToken) {
        this.oAuthToken = oAuthToken;
    }

    @PostConstruct
    public void calculateToken() throws IOException {
        HttpOAuthTokenKey httpOAuthTokenKey = getHttpOAuthTokenKey();
        token = oAuthToken.fetch(httpOAuthTokenKey);
        enrichIfNeeded();
    }

    protected abstract HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException;

    protected void enrichIfNeeded() {

    }

}
