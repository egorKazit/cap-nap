package com.yk.nap.handler;

import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.*;
import com.sap.cds.services.persistence.PersistenceService;
import com.yk.gen.threadreplicationservice.ProcessContext;
import com.yk.gen.threadreplicationservice.RevertContext;
import com.yk.gen.threadreplicationservice.ThreadReplicationService_;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.*;
import com.yk.nap.configuration.ParameterHolder;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ServiceName(ThreadService_.CDS_NAME)
public class ThreadHandler implements EventHandler {

    private final PersistenceService persistenceService;
    private final ApplicationService threadReplicationService;
    private final ParameterHolder parameterHolder;
    private AtomicInteger threadId;

    public ThreadHandler(PersistenceService persistenceService, @Qualifier(ThreadReplicationService_.CDS_NAME) ApplicationService threadReplicationService, ParameterHolder parameterHolder) {
        this.persistenceService = persistenceService;
        this.threadReplicationService = threadReplicationService;
        this.parameterHolder = parameterHolder;
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
            attachments.forEach(attachment -> attachment.setUrl("/odata/v4/ThreadService/Attachment" + "(ID=" + attachment.getId() + ",IsActiveEntity=" + attachment.getIsActiveEntity() + ")/content"));
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

    @Before(entity = Attachment_.CDS_NAME, event = DraftService.EVENT_DRAFT_NEW)
    public void enrichAttachments(@NonNull List<Attachment> attachments) {
        attachments.forEach(attachment -> {
            attachment.setId(UUID.randomUUID().toString());
            attachment.setUrl("/odata/v4/ThreadService/Attachment" + "(ID=" + attachment.getId() + ",IsActiveEntity=" + attachment.getIsActiveEntity() + ")/content");
        });
    }

    @Before(event = DraftService.EVENT_DRAFT_NEW, entity = Attachment_.CDS_NAME)
    public void checkContentType(@NonNull List<Attachment> attachments) {
        attachments.forEach(attachment -> {
            try {
                MimeTypes.getDefaultMimeTypes().forName(attachment.getMediaType());
            } catch (MimeTypeException e) {
                e.printStackTrace();
                throw new ServiceException("Unsupported type");
            }
        });
    }

    @SneakyThrows
    @On(event = DraftService.EVENT_DRAFT_PATCH, entity = Attachment_.CDS_NAME)
    public Attachment onAttachmentsUpload(DraftPatchEventContext context, @NonNull Attachment attachment) throws IOException {
        MimeTypes allMimeTypes = MimeTypes.getDefaultMimeTypes();
        MimeType mimeType;
        try {
            mimeType = allMimeTypes.forName(attachment.getMediaType());
        } catch (MimeTypeException e) {
            e.printStackTrace();
            throw new ServiceException("Unsupported type");
        }

        InputStream inputStream = attachment.getContent();
        if (inputStream == null) return null;

        String name = new File(parameterHolder.getDmsTargetFolder(), "thread-attachment-" + attachment.getId() + mimeType.getExtension()).getAbsolutePath();
        OutputStream outputStream = new FileOutputStream(name);
        outputStream.write(inputStream.readAllBytes());
        outputStream.flush();
        outputStream.close();
        attachment.setContent(null);
        context.setResult(List.of(attachment));
        context.setCompleted();
        return attachment;
    }

    @After(event = DraftService.EVENT_READ, entity = Attachment_.CDS_NAME)
    public void onAttachmentsRead(@NonNull CdsReadEventContext context) throws IOException, MimeTypeException {
        List<Attachment> attachments = persistenceService.run(Select.cqn(context.getCqn().toString()).columns(Attachment.ID, Attachment.MEDIA_TYPE, Attachment.FILE_NAME, Attachment.SIZE, Attachment.URL)).listOf(Attachment.class);
        if (attachments == null || attachments.size() != 1 || context.getCqn().items().stream().noneMatch(cqnSelectListItem -> cqnSelectListItem.asRef().segments().stream().anyMatch(segment -> segment.id().equals(Attachment.CONTENT))))
            return;
        Attachment attachment = attachments.get(0);
        MimeTypes allMimeTypes = MimeTypes.getDefaultMimeTypes();
        MimeType mimeType;
        try {
            mimeType = allMimeTypes.forName(attachment.getMediaType());
        } catch (MimeTypeException e) {
            e.printStackTrace();
            mimeType = new MimeTypes().forName(MimeTypes.PLAIN_TEXT);
        }
        String name = new File(parameterHolder.getDmsTargetFolder(), "thread-attachment-" + attachment.getId() + mimeType.getExtension()).getAbsolutePath();
        InputStream fileInputStream = new FileInputStream(name);
        attachment.setContent(fileInputStream);
        context.setResult(List.of(attachment));
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
