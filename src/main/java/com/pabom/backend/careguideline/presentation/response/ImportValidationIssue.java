package com.pabom.backend.careguideline.presentation.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImportValidationIssue(
        @Schema(example = "standards") String file,
        @Schema(example = "3") Integer row,
        @Schema(example = "species_code") String column,
        @Schema(example = "REFERENCE_NOT_FOUND") String code,
        @Schema(example = "등록되지 않은 종 코드입니다: UNKNOWN_SPECIES") String message
) {}
