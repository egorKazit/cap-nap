package com.yk.nap.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.yaml")
@Primary
public class ParameterHolder {

    @Value("${dms.type:local}")
    @Getter
    private String dmsType;

    @Value("${dms.target.folder}")
    @Getter
    private String dmsTargetFolder;

    @Value("${dms.target.credentials-file}")
    @Getter
    private String dmsTargetCredentialsFile;

    @Value("${security.credentials-file}")
    @Getter
    private String credentialsFile;

    @Value("${workflow.enabled:false}")
    @Getter
    private boolean workflowEnabled;

    @Value("${workflow.credentials:\"\"}")
    @Getter
    private String workflowCredentials;

    @Value("${workflow.uri:\"\"}")
    @Getter
    private String workflowUrl;

    @Value("${replication.enabled:false}")
    @Getter
    private boolean replicationEnabled;

    @Value("${replication.credentials-file:\"\"}")
    @Getter
    private String replicationCredentialsFile;

    @Value("${replication.destination-name:\"\"}")
    @Getter
    private String replicationDestinationName;

}
