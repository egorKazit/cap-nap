package com.yk.nap.service;

import com.yk.nap.configuration.ParameterHolder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Configuration
@NoArgsConstructor
public class AttachmentDriveOperatorFactory {

    @Bean
    public AttachmentDriveOperator getOperator(@NonNull ParameterHolder parameterHolder) throws GeneralSecurityException, IOException {
        if ("local".equals(parameterHolder.getDmsType())) {
            return new AttachmentLocalDriveOperator(parameterHolder);
        } else if ("google".equals(parameterHolder.getDmsType())) {
            return new AttachmentGoogleDriveOperator(parameterHolder);
        }
        return null;
    }

}
