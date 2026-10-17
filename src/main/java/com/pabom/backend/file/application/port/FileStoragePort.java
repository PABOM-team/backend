package com.pabom.backend.file.application.port;

import java.time.Duration;

public interface FileStoragePort {

    void upload(String storageKey, String contentType, byte[] content);

    String createDownloadUrl(String storageKey, Duration validity);

    void delete(String storageKey);
}
