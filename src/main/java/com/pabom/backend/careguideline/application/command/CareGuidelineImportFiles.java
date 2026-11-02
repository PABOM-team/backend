package com.pabom.backend.careguideline.application.command;

import org.springframework.web.multipart.MultipartFile;

public record CareGuidelineImportFiles(
        MultipartFile stages,
        MultipartFile items,
        MultipartFile standards,
        MultipartFile stageThresholds,
        MultipartFile descriptions
) {}
