package com.pabom.backend.file.application.service;

import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.application.result.FileDownloadResult;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.global.error.BusinessException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileQueryService {

    private final FileRepository fileRepository;
    private final FileStoragePort fileStoragePort;
    private final Clock clock;

    @Transactional(readOnly = true)
    public FileDownloadResult get(Long fileId, Long ownerId) {
        File file = fileRepository.findAccessibleByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
        Instant expiresAt = clock.instant().plus(FileUploadService.DOWNLOAD_URL_VALIDITY);
        String url = fileStoragePort.createDownloadUrl(
                file.getStorageKey(),
                FileUploadService.DOWNLOAD_URL_VALIDITY
        );
        return new FileDownloadResult(url, expiresAt, file.getContentType());
    }
}
