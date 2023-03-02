package com.yk.nap.service;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.yk.gen.threadservice.Attachment;
import com.yk.nap.configuration.ParameterHolder;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.springframework.scheduling.annotation.Async;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;


public class AttachmentGoogleDriveOperator implements AttachmentDriveOperator {


    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String DRIVE_FOLDER = "CapSync";

    private final Drive service;
    private final String capSyncFolderId;

    public AttachmentGoogleDriveOperator(ParameterHolder parameterHolder) throws IOException, GeneralSecurityException {

        NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        ServiceAccountCredentials sourceCredentials = ServiceAccountCredentials
                .fromStream(new FileInputStream(parameterHolder.getDmsTargetCredentialsFile()), () -> netHttpTransport);
        var credentials = sourceCredentials.createScoped(SCOPES);

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        service = new Drive.Builder(netHttpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(DRIVE_FOLDER).build();

        FileList capSyncFolder = service.files().list()
                .setQ(String.format("'root' in parents and trashed = false and name = '%s' and mimeType = 'application/vnd.google-apps.folder'", DRIVE_FOLDER))
                .execute();
        File capSyncFolderFile;

        if (capSyncFolder.getFiles().isEmpty()) {
            File fileMetadata = new File();
            fileMetadata.setName(DRIVE_FOLDER);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            capSyncFolderFile = service.files().create(fileMetadata).execute();
        } else {
            capSyncFolderFile = capSyncFolder.getFiles().get(0);
        }
        capSyncFolderId = capSyncFolderFile.getId();

    }

    @Override
    public String create(Attachment attachment) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(attachment.getFileName());
        fileMetadata.setParents(Collections.singletonList(capSyncFolderId));
        AbstractInputStreamContent inputStreamContent = new InputStreamContent(attachment.getMediaType(), InputStream.nullInputStream());
        return service.files().create(fileMetadata, inputStreamContent).execute().getId();
    }

    @Override
    public InputStream read(Attachment attachment) throws IOException {
        return service.files().get(attachment.getInternalQualifier())
                .executeMediaAsInputStream();
    }

    @Override
    @Async
    public void update(Attachment attachment) throws IOException {
        File file = service.files().get(attachment.getInternalQualifier()).execute();
        File newFile = file.clone();
        newFile.setId(null);
        String mimeType = file.getMimeType();
        service.files().update(attachment.getInternalQualifier(), newFile, new InputStreamContent(mimeType, attachment.getContent()))
                .execute();
    }

    @Override
    @Async
    public void delete(Attachment attachment) throws IOException {
        service.files().delete(attachment.getInternalQualifier()).execute();
    }
}
