package com.yk.nap.handler;

import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.*;
import com.sap.cds.services.persistence.PersistenceService;
import com.yk.gen.threadreplicationservice.ProcessContext;
import com.yk.gen.threadreplicationservice.RevertContext;
import com.yk.gen.threadreplicationservice.ThreadReplicationService_;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.*;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ServiceName(ThreadService_.CDS_NAME)
public class ThreadHandler implements EventHandler {

    private final PersistenceService persistenceService;
    private final ApplicationService threadReplicationService;
    private AtomicInteger threadId;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ThreadHandler(PersistenceService persistenceService, @Qualifier(ThreadReplicationService_.CDS_NAME) ApplicationService threadReplicationService) {
        this.persistenceService = persistenceService;
        this.threadReplicationService = threadReplicationService;
    }

    @Before(entity = Thread_.CDS_NAME, event = DraftService.EVENT_CREATE)
    @HandlerOrder(0)
    public void checkName(@NonNull List<Thread> threads, EventContext eventContext) {
        Optional<Row> alreadyExistingName = persistenceService.run(Select.from(Thread_.class).columns(CQL.get(Thread.THREAD), CQL.get(Thread.NAME)).where(persistedThread -> persistedThread.get(Thread.NAME).in(threads.stream().map(Thread::getName).toArray()))).first();
        if (alreadyExistingName.isPresent())
            throw new ServiceException("Name " + alreadyExistingName.get().get(Thread.NAME) + " already used in thread " + alreadyExistingName.get().get(Thread.THREAD));
    }

    @Before(entity = Thread_.CDS_NAME, event = DraftService.EVENT_CREATE)
    @HandlerOrder(10)
    public void setThreadSemanticId(@NonNull List<Thread> threads) {
        initCounter();
        threads.forEach(thread -> thread.setThread(String.valueOf(threadId.incrementAndGet())));
    }

    @After(entity = Thread_.CDS_NAME, event = {DraftService.EVENT_CREATE, DraftService.EVENT_UPDATE})
    public void setNoteSemanticId(@NonNull List<Thread> threads) {
        threads.stream().parallel().forEach(thread -> {
            AtomicInteger atomicInteger = new AtomicInteger();
            var notes = persistenceService.run(Select.from(Note_.class).columns(Note_::ID, Note_::createdAt).where(note -> note.thread_ID().eq(thread.getId())).orderBy(note -> note.createdAt().asc())).listOf(Note.class);
            notes.forEach(note -> note.setNote(String.valueOf(atomicInteger.incrementAndGet())));
            if (notes.isEmpty()) return;
            persistenceService.run(Update.entity(Note_.class).entries(notes));
        });

    }

    @After(entity = Thread_.CDS_NAME, event = {DraftService.EVENT_CREATE, DraftService.EVENT_UPDATE})
    public void updateAttachment(@NonNull List<Thread> threads) {
        threads.stream().parallel().forEach(thread -> {
            var attachments = thread.getAttachment();
            if (attachments == null) return;
            attachments.forEach(attachment -> attachment.setUrl("/odata/v4/ThreadService/Attachment(ID=" + attachment.getId() + ",IsActiveEntity=" + attachment.getIsActiveEntity() + ")/content"));
            if (attachments.isEmpty()) return;
            persistenceService.run(Update.entity(Attachment_.class).entries(attachments));
        });

    }

    @On(entity = Thread_.CDS_NAME, event = PublishContext.CDS_NAME)
    public void publish(@NonNull PublishContext publishContext) {
        List<Row> threads = persistenceService.run(publishContext.getCqn()).list();
        if (threads == null || threads.isEmpty()) throw new ServiceException("Can not process empty thread list");
        threads.stream().parallel().forEach(thread -> {
            ProcessContext processContext = ProcessContext.create();
            processContext.setThreadId(thread.get(Thread.ID).toString());
            threadReplicationService.emit(processContext);
            persistenceService.run(Update.entity(Thread_.class).data(Thread.STATUS, "Publishing").where(existingThread -> existingThread.ID().eq((String) thread.get(Thread.ID))));
        });
        publishContext.setCompleted();
    }

    @On(entity = Thread_.CDS_NAME, event = PromoteStatusContext.CDS_NAME)
    public void promoteStatus(@NonNull PromoteStatusContext promoteStatusContext) {
        Thread thread = persistenceService.run(promoteStatusContext.getCqn()).single(Thread.class);
        persistenceService.run(Update.entity(Thread_.CDS_NAME).data(Thread.STATUS, promoteStatusContext.getStatus()).where(conditionThread -> conditionThread.get(Thread.ID).eq(thread.getId())));
        promoteStatusContext.setCompleted();
    }

    @On(entity = Thread_.CDS_NAME, event = WithdrawContext.CDS_NAME)
    public void withdraw(@NonNull WithdrawContext withdrawContext) {
        List<Thread> threads = persistenceService.run(withdrawContext.getCqn()).listOf(Thread.class);
        threads.stream().parallel().forEach(thread -> {
            RevertContext revertContext = RevertContext.create();
            revertContext.setThreadId(thread.getId());
            threadReplicationService.emit(revertContext);
            persistenceService.run(Update.entity(Thread_.CDS_NAME).data(Thread.STATUS, "Initial").where(conditionThread -> conditionThread.get(Thread.ID).eq(thread.getId())));
        });
        withdrawContext.setCompleted();
    }

    @On(entity = Thread_.CDS_NAME, event = CompleteContext.CDS_NAME)
    public void complete(@NonNull CompleteContext publishContext) {
        List<Thread> threads = persistenceService.run(publishContext.getCqn()).listOf(Thread.class);
        if (threads == null || threads.isEmpty()) throw new ServiceException("Can not process empty thread list");
        threads.stream().parallel().forEach(thread -> persistenceService.run(Update.entity(Thread_.class).data(Thread.STATUS, "Completed").where(existingThread -> existingThread.ID().eq((String) thread.get(Thread.ID)))));
        publishContext.setCompleted();
    }

    private void initCounter() {
        if (threadId != null) {
            return;
        }
        threadId = new AtomicInteger();
        List<Row> selectResult = persistenceService.run(Select.from(Thread_.class).columns(CQL.max(CQL.get(Thread.THREAD)).as(Thread.ID))).list();
        if (selectResult.isEmpty()) threadId.set(0);
        else {

            threadId.set(selectResult.get(0).get(Thread.ID) != null ? Integer.parseInt((String) selectResult.get(0).get(Thread.ID)) : 0);
        }
    }

}
