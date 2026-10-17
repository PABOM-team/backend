package com.pabom.backend.file.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.application.result.FileDownloadResult;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.global.error.BusinessException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T01:00:00Z");
    private FileRepository repository;
    private FileStoragePort storage;
    private FileQueryService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(FileRepository.class);
        storage = org.mockito.Mockito.mock(FileStoragePort.class);
        service = new FileQueryService(repository, storage, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void returnsTenMinuteUrlForOwnedAccessibleFile() {
        File file = File.temporary(
                9L, FilePurpose.PET_PHOTO, "image/jpeg", 100, 10, 20,
                "files/9/key.jpg", NOW
        );
        given(repository.findAccessibleByIdAndOwnerId(301L, 9L)).willReturn(Optional.of(file));
        given(storage.createDownloadUrl("files/9/key.jpg", Duration.ofMinutes(10)))
                .willReturn("https://signed.example/file");

        FileDownloadResult result = service.get(301L, 9L);

        assertThat(result.url()).isEqualTo("https://signed.example/file");
        assertThat(result.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(10)));
        assertThat(result.contentType()).isEqualTo("image/jpeg");
        verify(storage).createDownloadUrl("files/9/key.jpg", Duration.ofMinutes(10));
    }

    @Test
    void hidesMissingOtherOwnersAndDeletedFilesAsFileNotFound() {
        given(repository.findAccessibleByIdAndOwnerId(301L, 9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(301L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(FileErrorCode.FILE_NOT_FOUND);
    }
}
