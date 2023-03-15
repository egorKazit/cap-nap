package com.yk.nap.service.attachment;

import com.yk.gen.threadservice.Attachment;

import java.io.IOException;
import java.io.InputStream;

public interface AttachmentDriveOperator {

    String create(Attachment attachment) throws IOException;

    InputStream read(Attachment attachment) throws IOException;

    void update(Attachment attachment) throws IOException;

    void delete(Attachment attachment) throws IOException;

}
