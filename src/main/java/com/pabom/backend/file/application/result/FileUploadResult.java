package com.pabom.backend.file.application.result;

import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.domain.type.FileStatus;
import java.time.Instant;

public record FileUploadResult(
        Long id,
        FilePurpose purpose,
        String contentType,
        long sizeBytes,
        int width,
        int height,
        String url,
        FileStatus status,
        Instant tempExpiresAt
) {
}
