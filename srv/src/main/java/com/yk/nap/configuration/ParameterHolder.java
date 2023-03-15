package com.yk.nap.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

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

    @Value("${workflow.credentials}")
    @Getter
    private String workflowCredentials;

    @Value("${workflow.uri}")
    @Getter
    private String workflowUrl;

}
