package com.yk.nap.configuration.destination;

import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DestinationHolderTest {

    @Test
    public void checkSetterGetter() {

        DestinationHolder.Destination destination = new DestinationHolder.Destination();
        destination.setName("name");
        assertEquals("name", destination.getName());
        destination.setModel("model");
        assertEquals("model", destination.getModel());

        DestinationHolder destinationHolder = new DestinationHolder();
        destinationHolder.setServices(List.of(destination));
        assertEquals(destination, destinationHolder.getServices().get(0));

        DestinationHolder.DestinationDetail destinationDetail = new DestinationHolder.DestinationDetail();
        destination.setDestination(destinationDetail);
        assertEquals(destinationDetail, destination.getDestination());

        destinationDetail.setName("nameDetails");
        assertEquals("nameDetails", destinationDetail.getName());

        destinationDetail.setType("type");
        assertEquals("type", destinationDetail.getType());
        destinationDetail.setRetrievalStrategy("retrievalStrategy");
        assertEquals("retrievalStrategy", destinationDetail.getRetrievalStrategy());
        destinationDetail.setSuffix("suffix");
        assertEquals("suffix", destinationDetail.getSuffix());
        destinationDetail.setService("service");
        assertEquals("service", destinationDetail.getService());
        destinationDetail.setUri("uri");
        assertEquals("uri", destinationDetail.getUri());
        destinationDetail.setCredentials("credentials");
        assertEquals("credentials", destinationDetail.getCredentials());

    }

}