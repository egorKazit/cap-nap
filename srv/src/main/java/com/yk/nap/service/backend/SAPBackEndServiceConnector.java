package com.yk.nap.service.backend;

import com.sap.conn.jco.JCoException;
import lombok.AllArgsConstructor;

public interface SAPBackEndServiceConnector {
    boolean replicateState(String id, State state) throws JCoException;

    @AllArgsConstructor
    enum State {
        REPLICATED(0), REVERTED(1);
        final int id;
    }

}
