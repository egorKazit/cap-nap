package com.yk.nap.service.attachment;

import com.yk.gen.threadservice.Attachment;
import com.yk.nap.configuration.ParameterHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
public class AttachmentLocalDriveOperatorTest {

    @InjectMocks
    private AttachmentLocalDriveOperator attachmentLocalDriveOperator;

    @Mock
    ParameterHolder parameterHolder;


    @BeforeEach
    public void prepare() throws IOException {
        String temporaryLocation = Files.createTempDirectory("temporaryLocation").toFile().getAbsolutePath();
        when(parameterHolder.getDmsTargetFolder()).thenReturn(temporaryLocation);
    }

    @Test
    public void processAttachment() throws IOException {

        // prepare attachment
        Attachment attachment = Attachment.create();
        attachment.setId(UUID.randomUUID().toString());

        // execute file creation
        var internalQualifier = attachmentLocalDriveOperator.create(attachment);
        attachment.setInternalQualifier(internalQualifier);

        // check that file has been created and empty
        assertTrue(new File(internalQualifier).exists());
        Path filePath = Path.of(internalQualifier);
        assertTrue(Files.readAllLines(filePath).isEmpty());

        // update attachment + file
        var contentStream = new ByteArrayInputStream("Test" .getBytes(StandardCharsets.UTF_8));
        attachment.setContent(contentStream);
        attachmentLocalDriveOperator.update(attachment);

        // check if file has been updated
        String fileContent = String.join("", Files.readAllLines(filePath));
        assertEquals("Test", fileContent);

        try (var fileContentStream = attachmentLocalDriveOperator.read(attachment)) {
            String fileContentFromPhysicalFile = new String(fileContentStream.readAllBytes());
            assertEquals(fileContent, fileContentFromPhysicalFile);
        }

        attachmentLocalDriveOperator.delete(attachment);
        assertFalse(new File(internalQualifier).exists());

    }

}