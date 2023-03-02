package com.yk.nap.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.yaml")
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

    @Value("${security.username}")
    @Getter(AccessLevel.PACKAGE)
    private String username;

    @Value("${security.password}")
    @Getter(AccessLevel.PACKAGE)
    private String password;

}
