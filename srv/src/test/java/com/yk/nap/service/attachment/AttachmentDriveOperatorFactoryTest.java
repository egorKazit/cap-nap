package com.yk.nap.service.attachment;

import com.yk.nap.configuration.ParameterHolder;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ExtendWith(MockitoExtension.class)
public class AttachmentDriveOperatorFactoryTest {

    @InjectMocks
    AttachmentDriveOperatorFactory attachmentDriveOperatorFactory;

    @Mock
    ParameterHolder parameterHolder;

    @Test
    public void getLocalAttachmentDriveOperator() throws GeneralSecurityException, IOException {
        when(parameterHolder.getDmsType()).thenReturn("local");
        assertTrue(attachmentDriveOperatorFactory.getOperator(parameterHolder) instanceof AttachmentLocalDriveOperator);
    }

    @Test
    public void getGoogleAttachmentDriveOperator() throws GeneralSecurityException, IOException {
        when(parameterHolder.getDmsType()).thenReturn("google");
        try (MockedConstruction<AttachmentGoogleDriveOperator> attachmentGoogleDriveOperatorMockedConstruction
                     = mockConstruction(AttachmentGoogleDriveOperator.class)) {
            assertTrue(attachmentDriveOperatorFactory.getOperator(parameterHolder) instanceof AttachmentGoogleDriveOperator);
        }
    }

    @Test
    public void getDefaultAttachmentDriveOperator() throws GeneralSecurityException, IOException {
        when(parameterHolder.getDmsType()).thenReturn("");
        assertTrue(attachmentDriveOperatorFactory.getOperator(parameterHolder) instanceof AttachmentLocalDriveOperator);
    }

}