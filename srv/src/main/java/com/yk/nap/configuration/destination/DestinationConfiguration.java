package com.yk.nap.configuration.destination;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ServiceName(ApplicationLifecycleService.DEFAULT_NAME)
@Order(0)
public class DestinationConfiguration implements EventHandler {

    private final DestinationHolder destinationHolder;
    private final DestinationFactory destinationFactory;
    private final DefaultDestinationLoader destinationLoader = new DefaultDestinationLoader();

    public DestinationConfiguration(DestinationHolder destinationHolder, DestinationFactory destinationFactory) {
        this.destinationHolder = destinationHolder;
        this.destinationFactory = destinationFactory;
    }

    @Before(event = ApplicationLifecycleService.EVENT_APPLICATION_PREPARED)
    public void initializeDestinations() {
        destinationHolder.getServices().forEach(destination -> destinationLoader.registerDestination(destinationFactory.getDestinationByName(destination.name)));
        DestinationAccessor.appendDestinationLoader(destinationLoader);
    }

}
