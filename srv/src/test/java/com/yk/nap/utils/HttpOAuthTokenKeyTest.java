package com.yk.nap.utils;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class HttpOAuthTokenKeyTest {

    @Test
    public void checkConstructor() {
        HttpOAuthTokenKey httpOAuthTokenKey = new HttpOAuthTokenKey(
                "", "", "",
                new HttpOAuthTokenKey.HttpOAuthTokenKeyUaa("", "", "", "", "", "", "")
        );
        assertNotNull(httpOAuthTokenKey);
    }

}