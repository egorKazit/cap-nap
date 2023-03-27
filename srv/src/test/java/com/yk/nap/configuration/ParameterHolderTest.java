package com.yk.nap.configuration;

import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class ParameterHolderTest {

    @Autowired
    private ParameterHolder parameterHolder;

    @Test
    public void getDmsType() {
        assertEquals("local", parameterHolder.getDmsType());
    }

    @Test
    public void getDmsTargetFolder() {
        assertEquals("/tmp", parameterHolder.getDmsTargetFolder());
    }

    @Test
    public void getDmsTargetCredentialsFile() {
        assertEquals("src/test/resources/test-google-cred-file.json", parameterHolder.getDmsTargetCredentialsFile());
    }

    @Test
    public void getCredentialsFile() {
        assertEquals("src/test/resources/creds.json", parameterHolder.getCredentialsFile());
    }

    @Test
    public void isWorkflowEnabled() {
        assertFalse(parameterHolder.isWorkflowEnabled());
    }

    @Test
    public void getWorkflowCredentials() {
        assertEquals("src/test/resources/test-workflow-destination.json", parameterHolder.getWorkflowCredentials());
    }

    @Test
    public void getWorkflowUrl() {
        assertEquals("http://localhost:8082", parameterHolder.getWorkflowUrl());
    }
}