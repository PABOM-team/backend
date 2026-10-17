package com.pabom.backend.file.application.result;

import java.time.Instant;

public record FileDownloadResult(String url, Instant expiresAt, String contentType) {
}
