package com.pabom.backend.species.presentation.response;

import com.pabom.backend.species.application.result.MorphResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record MorphResponse(
        @Schema(description = "모프 ID", example = "2") Long id,
        @Schema(description = "종 내 모프 코드", example = "HARLEQUIN") String code,
        @Schema(description = "모프 표시 이름", example = "할리퀸") String name,
        @Schema(description = "종 내 표시 순서", example = "2") int displayOrder
) {

    public static MorphResponse from(MorphResult result) {
        return new MorphResponse(result.id(), result.code(), result.name(), result.displayOrder());
    }
}
