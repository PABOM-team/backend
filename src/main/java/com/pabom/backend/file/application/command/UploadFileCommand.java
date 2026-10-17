package com.pabom.backend.file.application.command;

import com.pabom.backend.file.domain.type.FilePurpose;

public record UploadFileCommand(
        Long ownerId,
        FilePurpose purpose,
        String declaredContentType,
        byte[] content
) {
}
