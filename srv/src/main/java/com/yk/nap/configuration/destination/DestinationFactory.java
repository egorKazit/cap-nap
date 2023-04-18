package com.yk.nap.configuration.destination;

import com.google.gson.Gson;
import com.sap.cloud.sdk.cloudplatform.connectivity.AuthenticationType;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Header;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.yk.nap.utils.ExtensibleTokenKeeper;
import com.yk.nap.utils.HttpOAuthTokenKey;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.http.HttpHeaders;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
@Lazy
public class DestinationFactory {
    private final Map<String, HttpDestination> destinationBuilderMap = new HashMap<>();

    private final DestinationHolder destinationHolder;
    private final ApplicationContext applicationContext;

    HttpDestination getDestinationByName(String name) throws IOException {
        return destinationBuilderMap.get(name);
    }

    @PostConstruct
    void prepareAllDestinations() {

        if (destinationHolder == null || destinationHolder.getServices() == null)
            return;
        destinationHolder.getServices().forEach(destination -> {
            var defaultHttpDestination = DefaultHttpDestination
                    .builder(destination.destination.uri)
                    .authenticationType(AuthenticationType.NO_AUTHENTICATION)
                    .name(destination.destination.name).build();

            var defaultHttpDestinationProxyInvocationHandler = new DefaultHttpDestinationProxyInvocationHandler(defaultHttpDestination, applicationContext, destination);
            var defaultHttpDestinationProxy = (HttpDestination)
                    Proxy.newProxyInstance(DestinationFactory.class.getClassLoader(), new Class[]{HttpDestination.class}, defaultHttpDestinationProxyInvocationHandler);
            destinationBuilderMap.put(destination.name, defaultHttpDestinationProxy);

        });
    }

    public static class DefaultHttpDestinationProxyInvocationHandler extends ExtensibleTokenKeeper implements InvocationHandler {

        private final static String GET_HEADER_METHOD = "getHeaders";

        private final HttpDestination defaultHttpDestination;
        private final String credentials;

        public DefaultHttpDestinationProxyInvocationHandler(@NonNull HttpDestination defaultHttpDestination, @NonNull ApplicationContext applicationContext, @NonNull DestinationHolder.Destination destination) {
            super(applicationContext);
            this.defaultHttpDestination = defaultHttpDestination;
            this.credentials = destination.destination.credentials;
            assert Arrays.stream(this.defaultHttpDestination.getClass().getMethods()).anyMatch(method -> method.getName().equals(GET_HEADER_METHOD));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(@NonNull Object object, @NonNull Method method, Object[] objects) throws Throwable {
            var result = method.invoke(defaultHttpDestination, objects);
            if (method.getName().equals(GET_HEADER_METHOD)) {
                Collection<Header> headers = (Collection<Header>) result;
                // replace authorization
                headers.removeIf(header -> header.getName().equals(HttpHeaders.AUTHORIZATION));
                headers.add(new Header(HttpHeaders.AUTHORIZATION, getToken()));
            }
            return result;
        }

        @Override
        protected HttpOAuthTokenKey getHttpOAuthTokenKey() throws IOException {
            return new Gson().fromJson(String.join("", Files.readAllLines(Path.of(credentials))), HttpOAuthTokenKey.class);
        }
    }


}
