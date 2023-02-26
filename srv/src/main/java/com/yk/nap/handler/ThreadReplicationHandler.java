package com.yk.nap.handler;

import com.sap.cds.ql.Select;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.yk.gen.threadreplicationservice.ProcessContext;
import com.yk.gen.threadreplicationservice.RevertContext;
import com.yk.gen.threadservice.PromoteStatusContext;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.ThreadService_;
import com.yk.gen.threadservice.Thread_;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;

@Service
@ServiceName("ThreadReplicationService")
public class ThreadReplicationHandler implements EventHandler {

    private final ApplicationService threadReplicationService;

    public ThreadReplicationHandler(@Qualifier(ThreadService_.CDS_NAME) ApplicationService threadReplicationService) {
        this.threadReplicationService = threadReplicationService;
    }

    @On(event = ProcessContext.CDS_NAME)
    public void process(@NonNull ProcessContext processContext) {
        Executors.newSingleThreadExecutor().submit(() -> {
            // implement logic to synchronize
            try {
                synchronized (this) {
                    this.wait(3000);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            PromoteStatusContext promoteStatusContext = PromoteStatusContext.create();
            promoteStatusContext.setCqn(Select.from(Thread_.class).where(thread -> thread.get(Thread.ID).eq(processContext.getThreadId())));
            promoteStatusContext.setStatus("Published");
            threadReplicationService.emit(promoteStatusContext);
        });
        processContext.setCompleted();
    }

    @On(event = RevertContext.CDS_NAME)
    public void revert(@NonNull RevertContext revertContext) {

        revertContext.setCompleted();
    }

}
