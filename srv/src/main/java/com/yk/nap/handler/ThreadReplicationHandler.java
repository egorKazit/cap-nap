package com.yk.nap.handler;

import com.sap.cds.ql.Select;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.conn.jco.JCoException;
import com.yk.gen.com.sap.gateway.srvd_a2x.zykza_threadheader_api_def.v0001.*;
import com.yk.gen.threadreplicationservice.ProcessContext;
import com.yk.gen.threadreplicationservice.RevertContext;
import com.yk.gen.threadservice.PromoteStatusContext;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.ThreadService_;
import com.yk.gen.threadservice.Thread_;
import com.yk.nap.configuration.ParameterHolder;
import com.yk.nap.service.backend.SAPBackEndServiceConnector;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@ServiceName("ThreadReplicationService")
@EnableScheduling
public class ThreadReplicationHandler implements EventHandler {

    private final ApplicationService threadReplicationService;
    private final CqnService externalServiceOperator;
    private final ParameterHolder parameterHolder;
    private SAPBackEndServiceConnector sapBackEndServiceConnector;

    @SuppressWarnings("all")
    public ThreadReplicationHandler(@NonNull ApplicationContext applicationContext,
                                    @Qualifier(ThreadService_.CDS_NAME) ApplicationService threadReplicationService,
                                    @Qualifier(V0001_.CDS_NAME) CqnService externalServiceOperator,
                                    ParameterHolder parameterHolder) {
        this.threadReplicationService = threadReplicationService;
        this.externalServiceOperator = externalServiceOperator;
        this.parameterHolder = parameterHolder;
        if (parameterHolder.isReplicationEnabled())
            this.sapBackEndServiceConnector = applicationContext.getBean(SAPBackEndServiceConnector.class);
    }

    @On(event = ProcessContext.CDS_NAME)
    public void process(@NonNull ProcessContext processContext) throws JCoException {
        var sourceEntry = threadReplicationService.run(Select.from(Thread_.class)
                .columns(Thread_::ID, Thread_::thread, Thread_::name, thread -> thread.note().expand(), thread -> thread.attachment().expand())
                .where(thread -> thread.ID().eq(processContext.getThreadId()))).first(Thread.class).orElseThrow(() -> new ServiceException("Error on read"));
        var createEntityFromSourceContext = CreateEntityFromSourceContext.create();

        createEntityFromSourceContext.setSourceUUID(sourceEntry.getId());
        createEntityFromSourceContext.setThread(sourceEntry.getThread());
        createEntityFromSourceContext.setName(sourceEntry.getName());

        var items = Stream.concat(sourceEntry.getNote().stream().map(note -> {
            var noteItem = ZYKZDThreadItem.create();
            noteItem.setItem(Integer.valueOf(note.getNote()));
            noteItem.setType(String.valueOf(ItemType.NOTE.type));
            noteItem.setNameOrContent(note.getText());
            noteItem.setItemUUID(note.getId());
            return noteItem;
        }), sourceEntry.getAttachment().stream().map(attachment -> {
            var attachmentItem = ZYKZDThreadItem.create();
            attachmentItem.setType(String.valueOf(ItemType.ATTACHMENT.type));
            attachmentItem.setNameOrContent(attachment.getUrl());
            attachmentItem.setItemUUID(attachment.getId());
            return attachmentItem;
        })).toList();

        createEntityFromSourceContext.setItems(items);
        createEntityFromSourceContext.setCqn(Select.from(ZYKZAThreadHeader_.class));
        externalServiceOperator.emit(createEntityFromSourceContext);
        processContext.setResult(createEntityFromSourceContext.getResult().getUuid());

        processContext.setCompleted();

        if (parameterHolder.isReplicationEnabled() && sapBackEndServiceConnector != null)
            sapBackEndServiceConnector.replicateState(processContext.getThreadId(), SAPBackEndServiceConnector.State.REPLICATED);
    }

    @On(event = RevertContext.CDS_NAME)
    public void revert(@NonNull RevertContext revertContext) throws JCoException {
        var sourceThread = threadReplicationService.run(Select.from(Thread_.class)
                .where(thread -> thread.ID().eq(revertContext.getThreadId()))).listOf(Thread.class);
        sourceThread.forEach(thread -> {
            AbandonContext abandonContext = AbandonContext.create();
            abandonContext.setCqn(Select.from(ZYKZAThreadHeader_.class).where(zykzaThreadHeader -> zykzaThreadHeader.UUID().eq(thread.getReplicatedUUID())));
            externalServiceOperator.emit(abandonContext);
        });
        revertContext.setCompleted();
        if (parameterHolder.isReplicationEnabled() && sapBackEndServiceConnector != null)
            sapBackEndServiceConnector.replicateState(revertContext.getThreadId(), SAPBackEndServiceConnector.State.REVERTED);
    }

    @Scheduled(fixedDelay = 1000)
    public void processReplications() {

        if (!parameterHolder.isWorkflowEnabled())
            return;

        var replicatedEntries = externalServiceOperator
                .run(Select.from(ZYKZAThreadHeader_.class).where(zykzaThreadHeader -> zykzaThreadHeader.Status().eq("2")
                        .or(zykzaThreadHeader.Status().eq("3")).and(zykzaThreadHeader.ProcessedFlag().eq(false))))
                .listOf(ZYKZAThreadHeader.class);

        var targetThreads = threadReplicationService.run(Select.from(Thread_.class)
                        .where(thread -> thread.replicatedUUID().in(replicatedEntries.stream().map(ZYKZAThreadHeader::getUuid).toArray(String[]::new))))
                .listOf(Thread.class);

        targetThreads.parallelStream().forEach(targetThread -> {
            var replicatedEntryOptional = replicatedEntries.stream().filter(zykzaThreadHeader -> zykzaThreadHeader.getUuid().equals(targetThread.getReplicatedUUID()))
                    .findFirst();
            if (replicatedEntryOptional.isEmpty())
                throw new RuntimeException("MUST NOT HAPPEN NEVER");
            var completeContext = CompleteContext.create();
            completeContext.setCqn(Select.from(ZYKZAThreadHeader_.class).where(zykzaThreadHeader -> zykzaThreadHeader.UUID().eq(replicatedEntryOptional.get().getUuid())));
            externalServiceOperator.emit(completeContext);
            PromoteStatusContext promoteStatusContext = PromoteStatusContext.create();
            promoteStatusContext.setCqn(Select.from(Thread_.class).where(thread -> thread.ID().eq(targetThread.getId())));
            promoteStatusContext.setStatus(replicatedEntryOptional.get().getStatus().equals("2") ? "Published" : "Initial");
            threadReplicationService.emit(promoteStatusContext);
        });

    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private enum ItemType {
        NOTE('1'), ATTACHMENT('2');
        private final char type;
    }

}
