package com.pabom.backend.pet.presentation.response;

import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PetResponse(
        @Schema(description = "개체 ID", example = "12") Long id,
        @Schema(description = "종 ID", example = "1") Long speciesId,
        @Schema(description = "개체 이름", example = "콩이") String name,
        @Schema(description = "선택된 모프 목록") List<MorphResponse> morphs,
        @Schema(description = "기타 모프", example = "트라이컬러", nullable = true) String customMorph,
        @Schema(description = "부화일", example = "2026-03-02", type = "string", format = "date", nullable = true)
        LocalDate hatchedAt,
        @Schema(description = "성장 단계 ID", example = "1", nullable = true) Long lifeStageId,
        @Schema(description = "성장 단계 정확성", example = "CONFIRMED") Certainty stageCertainty,
        @Schema(description = "성별", example = "FEMALE") Sex sex,
        @Schema(description = "성별 정확성", example = "ESTIMATED") Certainty sexCertainty,
        @Schema(description = "사진 파일 ID", example = "301", nullable = true) Long photoFileId,
        @Schema(description = "생성 시각", example = "2026-10-18T07:52:18.222Z", type = "string", format = "date-time")
        Instant createdAt,
        @Schema(description = "수정 시각", example = "2026-10-18T07:52:18.222Z", type = "string", format = "date-time")
        Instant updatedAt
) {
    public static PetResponse from(PetResult result) {
        return new PetResponse(
                result.id(),
                result.speciesId(),
                result.name(),
                result.morphs().stream()
                        .map(item -> new MorphResponse(item.id(), item.code(), item.name()))
                        .toList(),
                result.customMorph(),
                result.hatchedAt(),
                result.lifeStageId(),
                result.stageCertainty(),
                result.sex(),
                result.sexCertainty(),
                result.photoFileId(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public record MorphResponse(
            @Schema(description = "모프 ID", example = "2") Long id,
            @Schema(description = "모프 코드", example = "HARLEQUIN") String code,
            @Schema(description = "모프 표시 이름", example = "할리퀸") String name
    ) {
    }
}
