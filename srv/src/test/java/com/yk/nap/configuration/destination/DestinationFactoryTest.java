package com.yk.nap.configuration.destination;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Header;
import com.yk.nap.service.oauth.OAuthToken;
import org.apache.http.HttpHeaders;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
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
    public void prepareDestinations() throws IOException {
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
        when(oAuthToken.fetch(any())).thenReturn("TestToken");

        var destinationFactory = new DestinationFactory(destinationHolder, applicationContext);
        destinationFactory.prepareAllDestinations();
        var httpDestination = destinationFactory.getDestinationByName(destination.name);
        var headers = httpDestination.getHeaders(URI.create(destination.destination.uri));
        Header header = headers.stream().filter(existingHeader -> existingHeader.getName().equals(HttpHeaders.AUTHORIZATION)).findFirst().orElseThrow();
        assertEquals("TestToken", header.getValue());

    }

}