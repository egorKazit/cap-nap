package com.yk.nap.service.attachment;

import com.yk.gen.threadservice.Attachment;
import com.yk.nap.configuration.ParameterHolder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.scheduling.annotation.Async;

import java.io.*;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AttachmentLocalDriveOperator implements AttachmentDriveOperator {


    private final ParameterHolder parameterHolder;

    @Override
    public String create(@NonNull Attachment attachment) throws IOException {
        return new File(parameterHolder.getDmsTargetFolder(), "thread-attachment-" + attachment.getId()).getAbsolutePath();
    }

    @Override
    public InputStream read(Attachment attachment) throws IOException {
        String name = new File(attachment.getInternalQualifier()).getAbsolutePath();
        return new FileInputStream(name);
    }

    @Override
    @Async
    public void update(@NonNull Attachment attachment) throws IOException {
        OutputStream outputStream = new FileOutputStream(attachment.getInternalQualifier());
        outputStream.write(attachment.getContent().readAllBytes());
        outputStream.flush();
        outputStream.close();
    }

    @Override
    @SuppressWarnings("all")
    @Async
    public void delete(@NonNull Attachment attachment) {
        new File(attachment.getInternalQualifier()).delete();
    }
}
