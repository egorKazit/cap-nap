package com.yk.nap.configuration.destination;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.yk.nap.service.oauth.OAuthToken;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class DestinationFactoryTest {

    @Test
    public void prepareNullDestinations() {
        DestinationHolder destinationHolder = mock(DestinationHolder.class);
        when(destinationHolder.getServices()).thenReturn(null);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        new DestinationFactory(destinationHolder, applicationContext).prepareAllDestinations();
    }

    @Test
    public void prepareDestinations() {
        DestinationHolder destinationHolder = mock(DestinationHolder.class);
        var destination = new DestinationHolder.Destination();
        destination.name = "name";
        destination.model = "model";
        destination.destination = new DestinationHolder.DestinationDetail();
        destination.destination.uri = "http://localhost:8082";
        destination.destination.name = "name";
        destination.destination.type = "type";
        destination.destination.service = "service";
        destination.destination.credentials = "src/test/resources/test-google-cred-file.json";
        when(destinationHolder.getServices()).thenReturn(List.of(destination));
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        OAuthToken oAuthToken = mock(OAuthToken.class);
        when(applicationContext.getBean(OAuthToken.class)).thenReturn(oAuthToken);
        DestinationFactory.DefaultHttpDestinationBuilderWithToken defaultHttpDestinationBuilderWithToken = mock(DestinationFactory.DefaultHttpDestinationBuilderWithToken.class);
        DefaultHttpDestination.Builder builder = mock(DefaultHttpDestination.Builder.class);
        when(defaultHttpDestinationBuilderWithToken.getBuilder()).thenReturn(builder);
        var targetDestination = DefaultHttpDestination.builder("").build();
        when(builder.build()).thenReturn(targetDestination);
        when(applicationContext.getBean((Class<Object>) any(), eq(applicationContext), eq(destination))).thenReturn(defaultHttpDestinationBuilderWithToken);
        var destinationFactory = new DestinationFactory(destinationHolder, applicationContext);
        destinationFactory.prepareAllDestinations();
        var destinationFromFactory = destinationFactory.getDestinationByName(destination.name);
        assertEquals(targetDestination, destinationFromFactory);
    }

    @Test
    public void checkDefaultHttpDestinationBuilderWithToken() throws IOException {
        OAuthToken oAuthToken = mock(OAuthToken.class);
        when(oAuthToken.fetch(any())).thenReturn("Token_hash");
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(OAuthToken.class)).thenReturn(oAuthToken);
        var destination = new DestinationHolder.Destination();
        destination.name = "name";
        destination.model = "model";
        destination.destination = new DestinationHolder.DestinationDetail();
        destination.destination.uri = "http://localhost:8082";
        destination.destination.name = "name";
        destination.destination.type = "type";
        destination.destination.service = "service";
        destination.destination.credentials = "src/test/resources/test-google-cred-file.json";
        DestinationFactory.DefaultHttpDestinationBuilderWithToken defaultHttpDestinationBuilderWithToken
                = new DestinationFactory.DefaultHttpDestinationBuilderWithToken(applicationContext, destination);
        var object = defaultHttpDestinationBuilderWithToken.getBuilder().build();
        assertEquals("http://localhost:8082", object.getUri().toString());
        assertEquals("name", object.getName().get());
    }

}