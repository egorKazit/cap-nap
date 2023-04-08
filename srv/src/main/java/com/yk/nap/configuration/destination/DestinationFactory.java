package com.yk.nap.configuration.destination;

import com.google.gson.Gson;
import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.yk.nap.utils.ExtensibleTokenKeeper;
import com.yk.nap.utils.HttpOAuthTokenKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.apache.http.HttpHeaders;
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
@Lazy
public class DestinationFactory {

    private final Map<String, DefaultHttpDestinationBuilderWithToken> destinationBuilderMap = new HashMap<>();

    private final DestinationHolder destinationHolder;
    private final ApplicationContext applicationContext;

    DefaultHttpDestination getDestinationByName(String name) {
        return destinationBuilderMap.get(name).getBuilder().build();
    }

    @PostConstruct
    void prepareAllDestinations() {
        if (destinationHolder == null || destinationHolder.getServices() == null)
            return;
        destinationHolder.getServices().forEach(destination -> {
            var defaultHttpDestinationBuilderWithToken = applicationContext.getBean(DefaultHttpDestinationBuilderWithToken.class, applicationContext, destination);
            destinationBuilderMap.put(destination.name, defaultHttpDestinationBuilderWithToken);
        });
    }

    @Service
    @Lazy
    public static class DefaultHttpDestinationBuilderWithToken extends ExtensibleTokenKeeper {
        @Getter
        private final DefaultHttpDestination.Builder builder;
        private final String credentials;


        DefaultHttpDestinationBuilderWithToken(@NonNull ApplicationContext applicationContext, @NonNull DestinationHolder.Destination destination) throws IOException {
            super(applicationContext);
            this.credentials = destination.destination.credentials;
            this.builder = DefaultHttpDestination
                    .builder(destination.destination.uri)
                    .authenticationType(AuthenticationType.NO_AUTHENTICATION)
                    .header(HttpHeaders.AUTHORIZATION, getToken())
                    .name(destination.destination.name);
        }

        @Override
        protected HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException {
            return new Gson().fromJson(String.join("", Files.readAllLines(Path.of(credentials))), HttpOAuthTokenKey.class);
        }

    }

}
