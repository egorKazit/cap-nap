package com.yk.nap.handler;

import com.sap.cds.ql.*;
import com.sap.cds.services.EventContext;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.cds.CdsDeleteEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.yk.gen.threadreplicationservice.ProcessContext;
import com.yk.gen.threadreplicationservice.RevertContext;
import com.yk.gen.threadreplicationservice.ThreadReplicationService_;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.*;
import com.yk.nap.configuration.ParameterHolder;
import com.yk.nap.service.workflow.WorkflowOperator;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@ServiceName(ThreadService_.CDS_NAME)
@Log4j2
public class ThreadHandler implements EventHandler {

    private final DraftService draftService;
    private final PersistenceService persistenceService;
    private final ApplicationService threadReplicationService;
    private final WorkflowOperator workflowOperator;
    private final ParameterHolder parameterHolder;
    private AtomicInteger threadId;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ThreadHandler(DraftService draftService, PersistenceService persistenceService, @Qualifier(ThreadReplicationService_.CDS_NAME) ApplicationService threadReplicationService, WorkflowOperator workflowOperator, ParameterHolder parameterHolder) {
        this.draftService = draftService;
        this.persistenceService = persistenceService;
        this.threadReplicationService = threadReplicationService;
        this.workflowOperator = workflowOperator;
        this.parameterHolder = parameterHolder;
    }

    @Before(entity = Thread_.CDS_NAME, event = DraftService.EVENT_CREATE)
    @HandlerOrder(0)
    public void checkName(@NonNull List<Thread> threads, EventContext eventContext) {
        Optional<Thread> alreadyExistingName = draftService.run(Select.from(Thread_.class)
                .columns(Thread_::thread, Thread_::name).where(persistedThread -> persistedThread.name()
                        .in(threads.stream().map(Thread::getName).toArray(String[]::new)))).first(Thread.class);
        if (alreadyExistingName.isPresent())
            throw new ServiceException("Name " + alreadyExistingName.get().getName() + " already used in thread " + alreadyExistingName.get().getThread());
    }

    @Before(entity = Thread_.CDS_NAME, event = DraftService.EVENT_CREATE)
    @HandlerOrder(10)
    public void setThreadSemanticId(@NonNull List<Thread> threads) {
        initCounter();
        threads.forEach(thread -> thread.setThread(String.valueOf(threadId.incrementAndGet())));
    }

    @Before(entity = Thread_.CDS_NAME, event = DraftService.EVENT_DELETE)
    public void validateEntriesBeforeDelete(@NonNull CdsDeleteEventContext eventContext) {
        var threadsToDelete = draftService.run(Select.from(eventContext.getCqn().ref())).listOf(Thread.class);
        threadsToDelete.forEach(thread -> {
            if (thread.getStatus().equals("Published")) {
                throw new ServiceException("Published thread can not be removed. Please complete " + thread.getName());
            }
        });
    }

    @On(entity = Thread_.CDS_NAME, event = CopyContext.CDS_NAME)
    public void copy(@NonNull CopyContext copyContext) {
        var sourceThread = draftService.run(copyContext.getCqn()).single(Thread.class);
        var sourceAttachments = draftService.run(Select.from(Attachment_.class).where(attachment -> attachment.get(Attachment.THREAD_ID).eq(sourceThread.getId()))).listOf(Attachment.class);
        var sourceNotes = draftService.run(Select.from(Note_.class).where(note -> note.get(Attachment.THREAD_ID).eq(sourceThread.getId()))).listOf(Note.class);
        var targetThread = Thread.create();
        targetThread.setName(sourceThread.getName() + " - copy");
        targetThread.setAttachment(sourceAttachments.stream().map(attachment -> {
            var targetAttachment = Attachment.create();
            targetAttachment.setFileName(attachment.getFileName());
            targetAttachment.setUrl(attachment.getUrl());
            targetAttachment.setSize(attachment.getSize());
            targetAttachment.setMediaType(attachment.getMediaType());
            targetAttachment.setThreadId(attachment.getThreadId());
            return targetAttachment;
        }).collect(Collectors.toList()));
        targetThread.setNote(sourceNotes.stream().map(note -> {
            var targetNote = Note.create();
            targetNote.setNote(note.getNote());
            targetNote.setText(note.getText());
            return targetNote;
        }).collect(Collectors.toList()));
        var targetDraftThread = draftService.newDraft(Insert.into(Thread_.class).entry(targetThread)).single(Thread.class);
        copyContext.setResult(targetDraftThread);
        copyContext.setCompleted();
    }

    @Before(entity = Thread_.CDS_NAME, event = PublishContext.CDS_NAME)
    public void prePublish(@NonNull PublishContext publishContext) {
        List<Thread> threads = persistenceService.run(Select.copy(publishContext.getCqn().asSelect())
                .columns(structuredType -> structuredType.get(Thread.ID),
                        structuredType -> structuredType.get(Thread.NAME),
                        StructuredType::expand)).listOf(Thread.class);
        threads.forEach(thread -> {
            int itemCount = thread.getAttachment().size() + thread.getNote().size();
            if (itemCount <= 0) {
                throw new ServiceException(String.format("Thread %s can not be published with empty items", thread.getName()));
            }
        });
    }

    @On(entity = Thread_.CDS_NAME, event = PublishContext.CDS_NAME)
    public void publish(@NonNull PublishContext publishContext) {
        List<Thread> threads = draftService.run(publishContext.getCqn()).listOf(Thread.class);
        if (threads == null || threads.isEmpty()) throw new ServiceException("Can not process empty thread list");
        threads.stream().parallel().forEach(thread -> {
            ProcessContext processContext = ProcessContext.create();
            processContext.setThreadId(thread.getId());
            threadReplicationService.emit(processContext);
            var replicatedUUID = processContext.getResult();
            draftService.run(Update.entity(Thread_.class)
                    .data(Map.of(Thread.STATUS, "Publishing", Thread.REPLICATED_UUID, replicatedUUID))
                    .where(existingThread -> existingThread.ID().eq(thread.getId())));
            if (parameterHolder.isWorkflowEnabled()) {
                try {
                    JSONArray items = new JSONArray();
                    List<Note> notes = draftService.run(Select.from(Note_.class).where(note -> note.thread_ID().eq(thread.getId()))).listOf(Note.class);
                    notes.forEach(note -> items.put(new JSONObject()
                            .put("Name", "Note " + note.getNote())
                            .put("Url", note.getText())));
                    List<Attachment> attachments = draftService.run(Select.from(Attachment_.class).where(note -> note.thread_ID().eq(thread.getId()))).listOf(Attachment.class);
                    attachments.forEach(attachment -> items.put(new JSONObject()
                            .put("Name", "Attachment: " + attachment.getFileName())
                            .put("Url", attachment.getUrl())));
                    WorkflowOperator.WorkflowPresentation workflowPresentation = workflowOperator
                            .startWorkflow("cap.rap.wf.caprapworkflow",
                                    new JSONObject()
                                            .put("thread", new JSONObject()
                                                    .put("UUID", replicatedUUID)
                                                    .put("Name", thread.getName())
                                                    .put("Items", items)));
                    draftService.run(Update.entity(Thread_.class)
                            .data(Map.of(Thread.WORKFLOW_UUID, workflowPresentation.getId()))
                            .where(existingThread -> existingThread.ID().eq(thread.getId())));
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        publishContext.setCompleted();
    }

    @On(entity = Thread_.CDS_NAME, event = PromoteStatusContext.CDS_NAME)
    public void promoteStatus(@NonNull PromoteStatusContext promoteStatusContext) {
        Thread thread = draftService.run(promoteStatusContext.getCqn()).single(Thread.class);
        draftService.run(Update.entity(Thread_.class).data(Thread.STATUS, promoteStatusContext.getStatus())
                .where(conditionThread -> conditionThread.ID().eq(thread.getId())));
        promoteStatusContext.setCompleted();
    }

    @On(entity = Thread_.CDS_NAME, event = WithdrawContext.CDS_NAME)
    public void withdraw(@NonNull WithdrawContext withdrawContext) {
        List<Thread> threads = draftService.run(withdrawContext.getCqn()).listOf(Thread.class);
        threads.stream().parallel().forEach(thread -> {
            if (thread.getStatus().equals("Published") || thread.getStatus().equals("Initial")) {
                withdrawContext.getMessages().warn("Entity already was processed. Withdrawn is not possible for thread " + thread.getName());
                return;
            }
            RevertContext revertContext = RevertContext.create();
            revertContext.setThreadId(thread.getId());
            threadReplicationService.emit(revertContext);
            try {
                if (workflowOperator.terminateWorkflow(thread.getWorkflowUUID())) {
                    log.atWarn().log("can not stop workflow with id " + thread.getWorkflowUUID());
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            draftService.run(Update.entity(Thread_.class).data(Thread.STATUS, "Initial")
                    .where(conditionThread -> conditionThread.ID().eq(thread.getId())));
        });
        withdrawContext.setCompleted();
    }

    @On(entity = Thread_.CDS_NAME, event = CompleteContext.CDS_NAME)
    public void complete(@NonNull CompleteContext publishContext) {
        List<Thread> threads = draftService.run(publishContext.getCqn()).listOf(Thread.class);
        if (threads == null || threads.isEmpty()) throw new ServiceException("Can not process empty thread list");
        threads.stream().parallel().forEach(thread -> draftService
                .run(Update.entity(Thread_.class).data(Thread.STATUS, "Completed").where(existingThread -> existingThread.ID().eq(thread.getId()))));
        publishContext.setCompleted();
    }

    private void initCounter() {
        if (threadId != null) {
            return;
        }
        threadId = new AtomicInteger();
        List<Thread> selectResult = draftService.run(Select.from(Thread_.class)
                .columns(CQL.max(CQL.get(Thread.THREAD)).as(Thread.ID))).listOf(Thread.class);
        if (selectResult.isEmpty()) threadId.set(0);
        else {

            threadId.set(selectResult.get(0).getId() != null ? Integer.parseInt(selectResult.get(0).getId()) : 0);
        }
    }

}
