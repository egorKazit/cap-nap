package com.yk.nap.configuration.destination;

import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ServiceName(ApplicationLifecycleService.DEFAULT_NAME)
@Order(0)
@Log4j2
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
        if (destinationHolder == null || destinationHolder.getServices() == null)
            return;
        destinationHolder.getServices().forEach(destination -> {
            try {
                destinationLoader.registerDestination(destinationFactory.getDestinationByName(destination.name));
            } catch (IOException e) {
                log.atError().log("Error on destination initialization");
            }
        });
        DestinationAccessor.appendDestinationLoader(destinationLoader);
    }

}
