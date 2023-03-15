package com.yk.nap.utils;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpOAuthTokenKey {
    public final String url;
    public final String username;
    public final String password;
    public final HttpOAuthTokenKeyUaa uaa;

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public final static class HttpOAuthTokenKeyUaa {
        @SerializedName("clientid")
        public final String clientId;
        @SerializedName("credential-type")
        public final String credentialType;
        @SerializedName("clientsecret")
        public final String clientSecret;
        @SerializedName("certurl")
        public final String certUrl;
        @SerializedName("certificate")
        public final String certificate;
        @SerializedName("key")
        public final String key;
        @SerializedName("tenantid")
        public final String tenantId;

    }

}
