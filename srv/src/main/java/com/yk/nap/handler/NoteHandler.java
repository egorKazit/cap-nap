package com.yk.nap.handler;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.services.draft.DraftService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.yk.gen.threadservice.Thread;
import com.yk.gen.threadservice.*;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ServiceName(ThreadService_.CDS_NAME)
@Log4j2
@AllArgsConstructor
public class NoteHandler implements EventHandler {

    private final PersistenceService persistenceService;

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

}
