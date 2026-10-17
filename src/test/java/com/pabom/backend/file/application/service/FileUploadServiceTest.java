package com.pabom.backend.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pabom.backend.file.application.command.UploadFileCommand;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.application.result.FileUploadResult;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import com.pabom.backend.global.error.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

class FileUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T01:00:00Z");

    private FileRepository fileRepository;
    private FileStoragePort fileStoragePort;
    private FileUploadService service;

    @BeforeEach
    void setUp() {
        fileRepository = org.mockito.Mockito.mock(FileRepository.class);
        fileStoragePort = org.mockito.Mockito.mock(FileStoragePort.class);
        service = new FileUploadService(
                fileRepository,
                fileStoragePort,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        given(fileRepository.save(any(File.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(fileStoragePort.createDownloadUrl(anyString(), any())).willReturn("https://signed.example/file");
    }

    @ParameterizedTest
    @EnumSource(value = FilePurpose.class, names = {"PET_PHOTO", "RECORD_PHOTO"})
    void uploadsSupportedPhotoAsTemporaryFile(FilePurpose purpose) throws Exception {
        byte[] image = image("jpg", 32, 24);

        FileUploadResult result = service.upload(
                new UploadFileCommand(7L, purpose, "image/jpeg", image)
        );

        assertThat(result.purpose()).isEqualTo(purpose);
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        assertThat(result.sizeBytes()).isEqualTo(image.length);
        assertThat(result.width()).isEqualTo(32);
        assertThat(result.height()).isEqualTo(24);
        assertThat(result.status()).isEqualTo(FileStatus.TEMP);
        assertThat(result.tempExpiresAt()).isEqualTo(NOW.plusSeconds(24 * 60 * 60));
        assertThat(result.url()).isEqualTo("https://signed.example/file");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStoragePort).upload(keyCaptor.capture(), org.mockito.ArgumentMatchers.eq("image/jpeg"),
                org.mockito.ArgumentMatchers.same(image));
        assertThat(keyCaptor.getValue()).startsWith("files/7/" + purpose.name().toLowerCase() + "/")
                .endsWith(".jpg");
        verify(fileStoragePort).createDownloadUrl(
                keyCaptor.getValue(),
                java.time.Duration.ofMinutes(10)
        );
    }

    @Test
    void rejectsImageLargerThanTenMegabytes() {
        byte[] content = new byte[(int) FileUploadService.MAX_IMAGE_SIZE_BYTES + 1];

        assertError(
                new UploadFileCommand(1L, FilePurpose.PET_PHOTO, "image/png", content),
                FileErrorCode.FILE_TOO_LARGE
        );
        verifyNoInteractions(fileRepository, fileStoragePort);
    }

    @Test
    void rejectsInvalidImageAndMimeMismatch() throws Exception {
        assertError(
                new UploadFileCommand(1L, FilePurpose.PET_PHOTO, "image/jpeg", "not-image".getBytes()),
                FileErrorCode.INVALID_FILE_FORMAT
        );
        assertError(
                new UploadFileCommand(1L, FilePurpose.PET_PHOTO, "image/jpeg", image("png", 3, 2)),
                FileErrorCode.INVALID_FILE_FORMAT
        );
        verifyNoInteractions(fileRepository, fileStoragePort);
    }

    @ParameterizedTest
    @EnumSource(value = FilePurpose.class, names = {"DEATH_EVIDENCE", "REPORT_RECEIPT"})
    void blocksSecondPhasePurposes(FilePurpose purpose) throws Exception {
        assertError(
                new UploadFileCommand(1L, purpose, "image/png", image("png", 2, 2)),
                FileErrorCode.UNSUPPORTED_FILE_PURPOSE
        );
        verifyNoInteractions(fileRepository, fileStoragePort);
    }

    @Test
    void deletesS3ObjectWhenDatabaseSaveFails() throws Exception {
        byte[] image = image("png", 4, 5);
        RuntimeException databaseFailure = new RuntimeException("database failure");
        given(fileRepository.save(any(File.class))).willThrow(databaseFailure);

        assertThatThrownBy(() -> service.upload(
                new UploadFileCommand(1L, FilePurpose.RECORD_PHOTO, "image/png", image)
        )).isSameAs(databaseFailure);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStoragePort).upload(keyCaptor.capture(), anyString(), any());
        verify(fileStoragePort).delete(keyCaptor.getValue());
    }

    @Test
    void doesNotSaveDatabaseWhenS3UploadFails() throws Exception {
        willThrow(new RuntimeException("s3 failure"))
                .given(fileStoragePort).upload(anyString(), anyString(), any());

        assertThatThrownBy(() -> service.upload(new UploadFileCommand(
                1L,
                FilePurpose.PET_PHOTO,
                "image/png",
                image("png", 2, 2)
        )))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(FileErrorCode.FILE_UPLOAD_FAILED);
        verify(fileRepository, never()).save(any());
    }

    private void assertError(UploadFileCommand command, FileErrorCode expected) {
        assertThatThrownBy(() -> service.upload(command))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(expected);
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
