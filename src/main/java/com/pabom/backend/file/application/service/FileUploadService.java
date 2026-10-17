package com.pabom.backend.file.application.service;

import com.pabom.backend.file.application.command.UploadFileCommand;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.application.result.FileUploadResult;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.error.FileErrorCode;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.global.error.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    static final Duration DOWNLOAD_URL_VALIDITY = Duration.ofMinutes(10);
    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );

    private final FileRepository fileRepository;
    private final FileStoragePort fileStoragePort;
    private final Clock clock;

    @Transactional
    public FileUploadResult upload(UploadFileCommand command) {
        validateCommand(command);
        ImageMetadata metadata = inspectImage(command.content(), command.declaredContentType());
        Instant uploadedAt = clock.instant();
        String storageKey = createStorageKey(command, metadata.contentType());

        uploadToStorage(storageKey, metadata.contentType(), command.content());
        boolean rollbackCleanupRegistered = registerRollbackCleanup(storageKey);
        try {
            File file = fileRepository.save(File.temporary(
                    command.ownerId(),
                    command.purpose(),
                    metadata.contentType(),
                    command.content().length,
                    metadata.width(),
                    metadata.height(),
                    storageKey,
                    uploadedAt
            ));
            String url = fileStoragePort.createDownloadUrl(storageKey, DOWNLOAD_URL_VALIDITY);
            return new FileUploadResult(
                    file.getId(),
                    file.getPurpose(),
                    file.getContentType(),
                    file.getSizeBytes(),
                    file.getWidth(),
                    file.getHeight(),
                    url,
                    file.getStatus(),
                    file.getTempExpiresAt()
            );
        } catch (RuntimeException exception) {
            if (!rollbackCleanupRegistered) {
                cleanup(storageKey, exception);
            }
            throw exception;
        }
    }

    private void validateCommand(UploadFileCommand command) {
        if (command.purpose() == null || !command.purpose().isUploadSupported()) {
            throw new BusinessException(FileErrorCode.UNSUPPORTED_FILE_PURPOSE);
        }
        if (command.content() == null || command.content().length == 0) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
        }
        if (command.content().length > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(FileErrorCode.FILE_TOO_LARGE);
        }
    }

    private ImageMetadata inspectImage(byte[] content, String declaredContentType) {
        String normalizedContentType = normalizeContentType(declaredContentType);
        if (!CONTENT_TYPE_EXTENSIONS.containsKey(normalizedContentType)) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(content)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
            }
            String detectedContentType = detectContentType(content);
            if (!normalizedContentType.equals(detectedContentType)) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
            }
            return new ImageMetadata(detectedContentType, image.getWidth(), image.getHeight());
        } catch (IOException exception) {
            throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT, exception);
        }
    }

    private String detectContentType(byte[] content) throws IOException {
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (stream == null) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
            }
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
            }
            var reader = readers.next();
            try {
                return switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "jpeg", "jpg" -> "image/jpeg";
                    case "png" -> "image/png";
                    default -> throw new BusinessException(FileErrorCode.INVALID_FILE_FORMAT);
                };
            } finally {
                reader.dispose();
            }
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parametersStart = contentType.indexOf(';');
        String value = parametersStart >= 0 ? contentType.substring(0, parametersStart) : contentType;
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String createStorageKey(UploadFileCommand command, String contentType) {
        return "files/%d/%s/%s.%s".formatted(
                command.ownerId(),
                command.purpose().name().toLowerCase(Locale.ROOT),
                UUID.randomUUID(),
                CONTENT_TYPE_EXTENSIONS.get(contentType)
        );
    }

    private void uploadToStorage(String storageKey, String contentType, byte[] content) {
        try {
            fileStoragePort.upload(storageKey, contentType, content);
        } catch (RuntimeException exception) {
            throw new BusinessException(FileErrorCode.FILE_UPLOAD_FAILED, exception);
        }
    }

    private boolean registerRollbackCleanup(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        fileStoragePort.delete(storageKey);
                    } catch (RuntimeException ignored) {
                        // The original transaction failure must remain the primary failure.
                    }
                }
            }
        });
        return true;
    }

    private void cleanup(String storageKey, RuntimeException originalException) {
        try {
            fileStoragePort.delete(storageKey);
        } catch (RuntimeException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }

    private record ImageMetadata(String contentType, int width, int height) {
    }
}
