package com.pabom.backend.pet.presentation.response;

import com.pabom.backend.pet.application.result.PetListResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record PetListResponse(
        @Schema(description = "개체 ID", example = "12") Long id,
        @Schema(description = "개체 이름", example = "콩이") String name,
        @Schema(description = "종 ID", example = "1") Long speciesId,
        @Schema(description = "종 표시 이름", example = "크레스티드 게코") String speciesName,
        @Schema(description = "선택된 모프 목록") List<PetResponse.MorphResponse> morphs,
        @Schema(description = "기타 모프", example = "트라이컬러", nullable = true) String customMorph,
        @Schema(description = "생애 단계 ID", example = "2", nullable = true) Long lifeStageId,
        @Schema(description = "생애 단계 표시 이름", example = "준성체", nullable = true) String lifeStageName,
        @Schema(description = "사진 파일 ID", example = "301", nullable = true) Long photoFileId
) {
    public static PetListResponse from(PetListResult result) {
        return new PetListResponse(
                result.id(),
                result.name(),
                result.speciesId(),
                result.speciesName(),
                result.morphs().stream()
                        .map(item -> new PetResponse.MorphResponse(item.id(), item.code(), item.name()))
                        .toList(),
                result.customMorph(),
                result.lifeStageId(),
                result.lifeStageName(),
                result.photoFileId()
        );
    }
}
