package com.yk.nap.service.attachment;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.yk.gen.threadservice.Attachment;
import com.yk.nap.configuration.ParameterHolder;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
public class AttachmentGoogleDriveOperatorTest {

    @Test
    public void processAttachment() throws IOException, GeneralSecurityException {

        var serviceAccountCredentials = mock(ServiceAccountCredentials.class);
        GoogleCredentials googleCredentials = mock(GoogleCredentials.class);
        when(serviceAccountCredentials.createScoped(any(Collection.class))).thenReturn(googleCredentials);

        Drive service = mock(Drive.class);
        Drive.Files files = mock(Drive.Files.class);
        Drive.Files.List list = mock(Drive.Files.List.class);
        when(list.setQ(anyString())).thenReturn(list);
        FileList fileList = mock(FileList.class);
        when(fileList.getFiles()).thenReturn(List.of());
        when(list.execute()).thenReturn(fileList);
        when(files.list()).thenReturn(list);
        when(service.files()).thenReturn(files);

        Drive.Files.Create create = mock(Drive.Files.Create.class);
        File file = mock(File.class);
        when(file.getId()).thenReturn("Id");
        when(create.execute()).thenReturn(file);
        when(files.create(any())).thenReturn(create);

        try (MockedStatic<ServiceAccountCredentials> serviceAccountCredentialsMockedStatic = mockStatic(ServiceAccountCredentials.class);
             MockedConstruction<Drive.Builder> ignored = mockConstruction(Drive.Builder.class, (builder, context) -> {
                 when(builder.setApplicationName(anyString())).thenReturn(builder);
                 when(builder.build()).thenReturn(service);
             })) {

            serviceAccountCredentialsMockedStatic.when(() ->
                    ServiceAccountCredentials.fromStream(any(), any())).thenReturn(serviceAccountCredentials);

            ParameterHolder parameterHolder = mock(ParameterHolder.class);
            when(parameterHolder.getDmsTargetCredentialsFile()).thenReturn("src/test/resources/test-google-cred-file.json");

            AttachmentGoogleDriveOperator attachmentGoogleDriveOperator = new AttachmentGoogleDriveOperator(parameterHolder);

            Attachment attachment = Attachment.create();
            attachment.setId(UUID.randomUUID().toString());

            when(files.create(any(), any())).thenReturn(create);
            when(file.getId()).thenReturn("ID1");
            when(create.execute()).thenReturn(file);
            assertEquals(attachmentGoogleDriveOperator.create(attachment), "ID1");

            Drive.Files.Update update = mock(Drive.Files.Update.class);
            when(update.execute()).thenReturn(file);
            when(files.update(any(), any(), any())).thenReturn(update);
            Drive.Files.Get get = mock(Drive.Files.Get.class);
            when(get.execute()).thenReturn(file);
            when(files.get(any())).thenReturn(get);
            when(file.setId(any())).thenReturn(file);
            when(file.getMimeType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
            when(file.clone()).thenReturn(file);
            attachment.setMediaType(MediaType.TEXT_PLAIN_VALUE);
            attachment.setContent(new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8)));
            attachmentGoogleDriveOperator.update(attachment);
            verify(update, times(1)).execute();

            when(get.executeMediaAsInputStream()).thenReturn(new ByteArrayInputStream("Test".getBytes(StandardCharsets.UTF_8)));
            try (InputStream inputStream = attachmentGoogleDriveOperator.read(attachment)) {
                assertEquals(new String(inputStream.readAllBytes()), "Test");
            }

            Drive.Files.Delete delete = mock(Drive.Files.Delete.class);
            when(files.delete(any())).thenReturn(delete);
            doNothing().when(delete).execute();
            attachmentGoogleDriveOperator.delete(attachment);
            verify(delete, times(1)).execute();

        }
    }

}