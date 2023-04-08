package com.yk.nap.service.backend;

import com.sap.conn.jco.*;
import com.sap.conn.jco.ext.Environment;
import com.yk.nap.configuration.ParameterHolder;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@SpringBootTest
public class SAPBackEndServiceConnectorImpTest {

    @Test(expected = JCoException.class)
    public void initializationWithoutFunction() throws JCoException, IOException {

        ParameterHolder parameterHolder = mock(ParameterHolder.class);

        try (MockedStatic<Environment> environmentMockedStatic = mockStatic(Environment.class)) {

            environmentMockedStatic.when(() -> Environment.registerDestinationDataProvider(any())).then(invocationOnMock -> null);
            JCoDestination jCoDestination = mock(JCoDestination.class);
            JCoRepository jCoRepository = mock(JCoRepository.class);
            when(jCoRepository.getFunction(anyString())).thenReturn(null);
            when(jCoDestination.getRepository()).thenReturn(jCoRepository);

            try (MockedStatic<JCoDestinationManager> jCoDestinationManagerMockedStatic = mockStatic(JCoDestinationManager.class)) {

                jCoDestinationManagerMockedStatic.when(() -> JCoDestinationManager.getDestination(any())).thenReturn(jCoDestination);
                when(parameterHolder.getReplicationCredentialsFile()).thenReturn("src/test/resources/test-rfc-replicator-props.txt");
                new SAPBackEndServiceConnectorImp(parameterHolder);

            }
        }
    }

    @Test
    public void replicateStateSuccessfully() throws JCoException, IOException {

        ParameterHolder parameterHolder = mock(ParameterHolder.class);

        try (MockedStatic<Environment> environmentMockedStatic = mockStatic(Environment.class)) {

            environmentMockedStatic.when(() -> Environment.registerDestinationDataProvider(any())).then(invocationOnMock -> null);
            JCoDestination jCoDestination = mock(JCoDestination.class);
            JCoRepository jCoRepository = mock(JCoRepository.class);
            JCoFunction jCoFunction = mock(JCoFunction.class);
            JCoParameterList jCoImportParameterList = mock(JCoParameterList.class);
            doNothing().when(jCoImportParameterList).setValue("IV_ID", "1");
            doNothing().when(jCoImportParameterList).setValue("IV_STATE", 0);
            when(jCoFunction.getImportParameterList()).thenReturn(jCoImportParameterList);
            JCoParameterList jCoExportParameterList = mock(JCoParameterList.class);
            when(jCoExportParameterList.getValue("EV_SUCCESS")).thenReturn("X");
            when(jCoFunction.getExportParameterList()).thenReturn(jCoExportParameterList);
            doNothing().when(jCoFunction).execute(jCoDestination);
            when(jCoRepository.getFunction(anyString())).thenReturn(jCoFunction);
            when(jCoDestination.getRepository()).thenReturn(jCoRepository);

            try (MockedStatic<JCoDestinationManager> jCoDestinationManagerMockedStatic = mockStatic(JCoDestinationManager.class)) {

                jCoDestinationManagerMockedStatic.when(() -> JCoDestinationManager.getDestination(any())).thenReturn(jCoDestination);
                when(parameterHolder.getReplicationCredentialsFile()).thenReturn("src/test/resources/test-rfc-replicator-props.txt");
                var sapBackEndServiceConnector = new SAPBackEndServiceConnectorImp(parameterHolder);
                assertTrue(sapBackEndServiceConnector.replicateState("1", SAPBackEndServiceConnector.State.REPLICATED));

            }
        }
    }

    @Test
    public void replicateStateWithError() throws JCoException, IOException {

        ParameterHolder parameterHolder = mock(ParameterHolder.class);

        try (MockedStatic<Environment> environmentMockedStatic = mockStatic(Environment.class)) {

            environmentMockedStatic.when(() -> Environment.registerDestinationDataProvider(any())).then(invocationOnMock -> null);
            JCoDestination jCoDestination = mock(JCoDestination.class);
            JCoRepository jCoRepository = mock(JCoRepository.class);
            JCoFunction jCoFunction = mock(JCoFunction.class);
            JCoParameterList jCoImportParameterList = mock(JCoParameterList.class);
            doNothing().when(jCoImportParameterList).setValue("IV_ID", "1");
            doNothing().when(jCoImportParameterList).setValue("IV_STATE", 0);
            when(jCoFunction.getImportParameterList()).thenReturn(jCoImportParameterList);
            JCoParameterList jCoExportParameterList = mock(JCoParameterList.class);
            when(jCoExportParameterList.getValue("EV_SUCCESS")).thenReturn("");
            when(jCoFunction.getExportParameterList()).thenReturn(jCoExportParameterList);
            doNothing().when(jCoFunction).execute(jCoDestination);
            when(jCoRepository.getFunction(anyString())).thenReturn(jCoFunction);
            when(jCoDestination.getRepository()).thenReturn(jCoRepository);

            try (MockedStatic<JCoDestinationManager> jCoDestinationManagerMockedStatic = mockStatic(JCoDestinationManager.class)) {

                jCoDestinationManagerMockedStatic.when(() -> JCoDestinationManager.getDestination(any())).thenReturn(jCoDestination);
                when(parameterHolder.getReplicationCredentialsFile()).thenReturn("src/test/resources/test-rfc-replicator-props.txt");
                var sapBackEndServiceConnector = new SAPBackEndServiceConnectorImp(parameterHolder);
                assertFalse(sapBackEndServiceConnector.replicateState("1", SAPBackEndServiceConnector.State.REPLICATED));

            }
        }
    }

}