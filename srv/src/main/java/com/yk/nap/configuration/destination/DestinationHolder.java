package com.yk.nap.configuration.destination;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cds.remote")
@Getter
@Setter
class DestinationHolder {

    private List<Destination> services;

    @Getter
    @Setter
    static class Destination {
        String name;
        String model;
        DestinationDetail destination;
    }

    @Getter
    @Setter
    static class DestinationDetail {
        String name;
        String type;
        String retrievalStrategy;
        String suffix;
        String service;
        String uri;
        String credentials;
    }

}
