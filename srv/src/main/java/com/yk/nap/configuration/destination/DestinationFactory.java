package com.yk.nap.configuration.destination;

import com.google.gson.Gson;
import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.yk.nap.service.oauth.OAuthToken;
import com.yk.nap.utils.ExtensibleTokenKeeper;
import com.yk.nap.utils.HttpOAuthTokenKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class DestinationFactory {

    private final Map<String, DefaultHttpDestinationBuilderWithToken> destinationBuilderMap = new HashMap<>();

    private final DestinationHolder destinationHolder;
    private final ApplicationContext applicationContext;

    DefaultHttpDestination getDestinationByName(String name) {
        return destinationBuilderMap.get(name).builder.build();
    }

    @PostConstruct
    void prepareAllDestinations() {
        destinationHolder.getServices().forEach(destination -> {
            var oAuthToken = applicationContext.getBean(OAuthToken.class);
            var defaultHttpDestinationBuilderWithToken = applicationContext.getBean(DefaultHttpDestinationBuilderWithToken.class, oAuthToken, destination);
            destinationBuilderMap.put(destination.name, defaultHttpDestinationBuilderWithToken);
        });
    }

    @Service
    @Lazy
    private static class DefaultHttpDestinationBuilderWithToken extends ExtensibleTokenKeeper {
        @Getter
        private final DefaultHttpDestination.Builder builder;
        private final String credentials;


        private DefaultHttpDestinationBuilderWithToken(@NonNull OAuthToken oAuthToken, @NonNull DestinationHolder.Destination destination) throws IOException {
            super(oAuthToken);
            this.credentials = destination.destination.credentials;
            this.builder = DefaultHttpDestination
                    .builder(destination.destination.uri)
                    .authenticationType(AuthenticationType.NO_AUTHENTICATION)
                    .header("Authorization", getToken())
                    .name(destination.destination.name);
        }

        @Override
        protected HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException {
            return new Gson().fromJson(String.join("", Files.readAllLines(Path.of(credentials))), HttpOAuthTokenKey.class);
        }

    }

}
