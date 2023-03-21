package com.yk.nap.handler;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.cqn.CqnReference;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.draft.DraftPatchEventContext;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.HandlerOrder;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.*;
import com.yk.nap.service.attachment.AttachmentDriveOperator;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@ServiceName(ThreadService_.CDS_NAME)
@Log4j2
public class AttachmentHandler implements EventHandler {

    private final DraftService draftService;
    private final AttachmentDriveOperator attachmentDriveOperator;

    @SuppressWarnings("all")
    public AttachmentHandler(DraftService draftService,
                             AttachmentDriveOperator attachmentDriveOperator) {
        this.draftService = draftService;
        this.attachmentDriveOperator = attachmentDriveOperator;
    }

    @Before(event = DraftService.EVENT_DRAFT_NEW, entity = Attachment_.CDS_NAME)
    @HandlerOrder(0)
    public void checkContentType(@NonNull List<Attachment> attachments) {
        attachments.forEach(attachment -> {
            try {
                MimeTypes.getDefaultMimeTypes().forName(attachment.getMediaType());
            } catch (MimeTypeException mimeTypeException) {
                log.atError().log("Error at creation: " + mimeTypeException.getMessage());
                throw new ServiceException("Unsupported type for file " + attachment.getFileName());
            }
        });
    }

    @Before(entity = Attachment_.CDS_NAME, event = DraftService.EVENT_DRAFT_NEW)
    @HandlerOrder(10)
    public void enrichAttachments(@NonNull List<Attachment> attachments) {
        attachments.forEach(attachment -> {
            attachment.setId(UUID.randomUUID().toString());
            attachment.setUrl("/odata/v4/ThreadService/Attachment" + "(ID=" + attachment.getId() + ",IsActiveEntity=" + attachment.getIsActiveEntity() + ")/content");
            try {
                attachment.setInternalQualifier(attachmentDriveOperator.create(attachment));
            } catch (IOException mimeTypeException) {
                log.atError().log("Error at creation: " + mimeTypeException.getMessage());
                throw new ServiceException("Unsupported type for file " + attachment.getFileName());
            }
        });
    }

    @Before(entity = Attachment_.CDS_NAME, event = DraftService.EVENT_DRAFT_PATCH)
    public void onAttachmentsUpload(@NonNull DraftPatchEventContext context, @NonNull Attachment attachment) {
        Attachment attachmentToProcess = draftService.run(Select.from(context.getCqn().asUpdate().ref())).single(Attachment.class);
        InputStream inputStream = attachment.getContent();
        if (inputStream != null) {
            attachmentToProcess.setContent(inputStream);
            try {
                attachmentDriveOperator.update(attachmentToProcess);
            } catch (IOException ioException) {
                log.atError().log("Error on upload: " + ioException.getMessage());
                throw new ServiceException("Error at content processing of file " + attachment.getFileName());
            }
            attachment.setContent(null);
        }
    }

    @Before(entity = Thread_.CDS_NAME, event = DraftService.EVENT_DRAFT_SAVE)
    public void eraseAttachment(@NonNull DraftActivateContext eventContext) {

        List<Thread> threads = draftService.run(Select.from(eventContext.getCqn().ref()).columns(Thread.ID)).listOf(Thread.class);
        var activeThreadsWithAttachments = draftService.run(Select.from(Thread_.class).columns(Thread_::ID, thread -> thread.attachment().expand())
                .where(thread -> thread.ID().in(threads.stream().map(Thread::getId).collect(Collectors.toList())).and(thread.IsActiveEntity().eq(true)))).listOf(Thread.class);
        var draftThreadsWithAttachments = draftService.run(Select.from(Thread_.class).columns(Thread_::ID, thread -> thread.attachment().expand())
                .where(thread -> thread.ID().in(threads.stream().map(Thread::getId).collect(Collectors.toList())).and(thread.IsActiveEntity().eq(false)))).listOf(Thread.class);
        draftThreadsWithAttachments.forEach(draftThreadWithAttachments -> {
            var activeThreadsWithAttachmentsOptional = activeThreadsWithAttachments.stream().filter(thread -> thread.getId().equals(draftThreadWithAttachments.getId())).findFirst();
            if (activeThreadsWithAttachmentsOptional.isEmpty())
                return;
            var activeThreadWithAttachments = activeThreadsWithAttachmentsOptional.get();
            activeThreadWithAttachments.getAttachment().parallelStream().forEach(activeAttachment -> {
                if (draftThreadWithAttachments.getAttachment().stream().noneMatch(draftAttachment -> draftAttachment.getId().equals(activeAttachment.getId()))) {
                    try {
                        attachmentDriveOperator.delete(activeAttachment);
                    } catch (IOException ioException) {
                        log.atError().log("Error at removing: " + ioException.getMessage());
                        throw new ServiceException("Can not delete file " + activeAttachment.getFileName());
                    }
                }
            });
        });

    }

    @After(event = DraftService.EVENT_READ, entity = Attachment_.CDS_NAME)
    public void onAttachmentsDownload(@NonNull CdsReadEventContext context) {
        var requestedFields = context.getCqn().items().stream().flatMap(cqnSelectListItem ->
                cqnSelectListItem.isRef() ?
                        cqnSelectListItem.asRef().segments().stream()
                                .map(CqnReference.Segment::id) : Stream.empty()).sorted().toList();
        var expectedFields = Stream.of(Attachment.CONTENT, Attachment.MEDIA_TYPE).sorted().toList();
        if (!requestedFields.equals(expectedFields))
            return;
        Attachment attachment = draftService
                .run(Select.cqn(context.getCqn().toString()).columns(Attachment.ID, Attachment.INTERNAL_QUALIFIER,
                        Attachment.MEDIA_TYPE, Attachment.FILE_NAME, Attachment.SIZE, Attachment.URL))
                .first(Attachment.class).orElseThrow(() -> new ServiceException(""));
        try {
            attachment.setContent(attachmentDriveOperator.read(attachment));
        } catch (IOException ioException) {
            log.atError().log("Error on download: " + ioException.getMessage());
            throw new ServiceException("Error at content reading of file " + attachment.getFileName());
        }
        context.setResult(List.of(attachment));
    }

    @After(entity = Thread_.CDS_NAME, event = {DraftService.EVENT_CREATE, DraftService.EVENT_UPDATE})
    public void updateAttachment(@NonNull List<Thread> threads) {
        threads.stream().parallel().forEach(thread -> {
            var attachments = thread.getAttachment();
            if (attachments == null) return;
            attachments.forEach(attachment -> attachment.setUrl("/odata/v4/ThreadService/Attachment(ID=" + attachment.getId() + ",IsActiveEntity=" + attachment.getIsActiveEntity() + ")/content"));
            if (attachments.isEmpty()) return;
            draftService.run(Update.entity(Attachment_.class).entries(attachments));
        });

    }

}
