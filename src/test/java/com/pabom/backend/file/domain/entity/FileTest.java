package com.pabom.backend.file.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FileTest {

    @Test
    void createsTemporaryFileExpiringAfterTwentyFourHours() {
        Instant now = Instant.parse("2026-10-24T01:00:00Z");

        File file = File.temporary(
                1L, FilePurpose.RECORD_PHOTO, "image/png", 100, 20, 30, "key", now
        );

        assertThat(file.getStatus()).isEqualTo(FileStatus.TEMP);
        assertThat(file.getTempExpiresAt()).isEqualTo(now.plusSeconds(24 * 60 * 60));
    }

    @Test
    void marksFileDeletedWithoutRemovingStorageMetadata() {
        File file = File.temporary(
                1L, FilePurpose.RECORD_PHOTO, "image/png", 100, 20, 30, "key",
                Instant.parse("2026-10-24T01:00:00Z")
        );

        file.delete();

        assertThat(file.getStatus()).isEqualTo(FileStatus.DELETED);
        assertThat(file.getStorageKey()).isEqualTo("key");
    }
}
