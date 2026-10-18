package com.pabom.backend.species.presentation.response;

import com.pabom.backend.species.application.result.SpeciesResult;
import com.pabom.backend.species.domain.type.ReptileCategory;
import io.swagger.v3.oas.annotations.media.Schema;

public record SpeciesResponse(
        @Schema(description = "종 ID", example = "1") Long id,
        @Schema(description = "종 코드", example = "CRESTED") String code,
        @Schema(description = "종 표시 이름", example = "크레스티드 게코") String commonName,
        @Schema(description = "파충류 분류", example = "LIZARD") ReptileCategory category,
        @Schema(description = "분류 내 표시 순서", example = "1") int displayOrder
) {
    public static SpeciesResponse from(SpeciesResult result) {
        return new SpeciesResponse(
                result.id(), result.code(), result.commonName(), result.category(), result.displayOrder()
        );
    }
}
