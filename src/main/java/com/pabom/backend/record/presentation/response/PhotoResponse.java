package com.pabom.backend.record.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PhotoResponse(Long fileId, @Schema(format = "uri") String url) {}
