package com.yk.nap.configuration.destination;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
public class DestinationConfigurationTest {


    @Test
    public void processNullDestinations() {
        DestinationFactory destinationFactory = mock(DestinationFactory.class);
        new DestinationConfiguration(null, destinationFactory).initializeDestinations();
    }

    @Test
    public void processEmptyDestinations() {
        DestinationHolder destinationHolder = mock(DestinationHolder.class);
        DestinationFactory destinationFactory = mock(DestinationFactory.class);
        when(destinationHolder.getServices()).thenReturn(null);
        new DestinationConfiguration(destinationHolder, destinationFactory).initializeDestinations();
        verify(destinationHolder, times(1)).getServices();
    }

    @Test
    public void processDestinations() throws IOException {
        DestinationHolder destinationHolder = mock(DestinationHolder.class);
        DestinationFactory destinationFactory = mock(DestinationFactory.class);
        when(destinationFactory.getDestinationByName(any())).thenReturn(DefaultHttpDestination.builder("http://localhost:8082").build());
        when(destinationHolder.getServices()).thenReturn(List.of(new DestinationHolder.Destination()));
        AtomicInteger callsCount = new AtomicInteger();
        try (MockedConstruction<DefaultDestinationLoader> ignored
                     = mockConstruction(DefaultDestinationLoader.class, (defaultDestinationLoader, context) ->
                when(defaultDestinationLoader.registerDestination(any(DefaultHttpDestination.class))).then(invocationOnMock -> {
                    callsCount.incrementAndGet();
                    return null;
                }))) {
            new DestinationConfiguration(destinationHolder, destinationFactory).initializeDestinations();
        }
        assertEquals(1, callsCount.get());
        verify(destinationHolder, times(2)).getServices();
    }

}