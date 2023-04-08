package com.yk.nap.utils;

import com.yk.nap.service.oauth.OAuthToken;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

public abstract class ExtensibleTokenKeeper {

    private final ApplicationContext applicationContext;

    protected ExtensibleTokenKeeper(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected String getToken() throws IOException {
        HttpOAuthTokenKey httpOAuthTokenKey = getHttpOAuthTokenKey();
        return applicationContext.getBean(OAuthToken.class).fetch(httpOAuthTokenKey);
    }

    protected abstract HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException;

}
