package com.pabom.backend.careguideline.presentation.response;

import java.util.List;

public record CareGuidelineImportErrorDetails(
        CareGuidelineImportResponse.ImportCounts counts,
        List<ImportValidationIssue> errors
) {}
