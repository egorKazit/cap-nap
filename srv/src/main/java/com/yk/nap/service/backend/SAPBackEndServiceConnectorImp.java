package com.yk.nap.service.backend;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;
import com.sap.conn.jco.ext.DataProviderException;
import com.sap.conn.jco.ext.DestinationDataEventListener;
import com.sap.conn.jco.ext.DestinationDataProvider;
import com.sap.conn.jco.ext.Environment;
import com.yk.nap.configuration.ParameterHolder;
import lombok.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

@Service
@Scope("singleton")
@ConditionalOnProperty(prefix = "replication", name = "enabled", havingValue = "true")
@Lazy
public class SAPBackEndServiceConnectorImp implements SAPBackEndServiceConnector {

    private final JCoDestination destination;
    private final JCoFunction function;

    public SAPBackEndServiceConnectorImp(@NonNull ParameterHolder parameterHolder) throws JCoException, IOException {
        Properties connectProperties = new Properties();
        String parameters = new String(Files.readAllBytes(Path.of(parameterHolder.getReplicationCredentialsFile())));
        Stream.of(parameters.split("\n")).forEach(parameterLine -> {
            int firstOccurrenceOfEqual = parameterLine.indexOf("=");
            String key = parameterLine.substring(0, firstOccurrenceOfEqual);
            String value = parameterLine.substring(firstOccurrenceOfEqual + 1);
            connectProperties.setProperty(key, value);
        });

        DestinationDataProvider destinationDataProvider = new DestinationDataProvider() {
            @Override
            public Properties getDestinationProperties(String s) throws DataProviderException {
                return connectProperties;
            }

            @Override
            public boolean supportsEvents() {
                return false;
            }

            @Override
            public void setDestinationDataEventListener(DestinationDataEventListener destinationDataEventListener) {

            }
        };
        Environment.registerDestinationDataProvider(destinationDataProvider);
        destination = JCoDestinationManager.getDestination(parameterHolder.getReplicationDestinationName());
        function = destination.getRepository().getFunction("ZYKZ_STATE_RFC_TRACKER");
        if (function == null)
            throw new JCoException(JCoException.JCO_ERROR_FUNCTION_NOT_FOUND, "Can not RFC trace function");
    }

    @Override
    public boolean replicateState(@NonNull String id, @NonNull State state) throws JCoException {
        function.getImportParameterList().setValue("IV_ID", id);
        function.getImportParameterList().setValue("IV_STATE", state.id);
        function.execute(destination);
        var successFlag = function.getExportParameterList().getValue("EV_SUCCESS");
        return "X".equals(successFlag);
    }

}
