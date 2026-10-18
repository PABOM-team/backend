package com.pabom.backend.pet.presentation.request;

import com.pabom.backend.pet.application.command.CreatePetCommand;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePetRequest(
        @Schema(description = "중복 등록 방지용 요청 UUID", example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID clientRequestId,
        @Schema(description = "종 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Long speciesId,
        @Schema(description = "개체 이름. 앞뒤 공백 제거 후 1~20자", example = "콩이",
                minLength = 1, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 20) String name,
        @Schema(description = "선택한 모프 ID 목록. 생략, null 또는 빈 배열 허용", example = "[2, 4]", nullable = true)
        List<@NotNull Long> morphIds,
        @Schema(description = "기타 모프 직접 입력. 최대 40자", example = "트라이컬러", maxLength = 40, nullable = true)
        @Size(max = 40) String customMorph,
        @Schema(description = "부화일. 모르면 null", example = "2026-03-02", type = "string", format = "date", nullable = true)
        @PastOrPresent LocalDate hatchedAt,
        @Schema(description = "성장 단계 ID. stageCertainty가 CONFIRMED 또는 ESTIMATED이면 필수이며, UNKNOWN이면 null이어야 함",
                example = "1", nullable = true)
        Long lifeStageId,
        @Schema(description = "성장 단계 정확성. 생략 또는 null이면 UNKNOWN. UNKNOWN은 lifeStageId null만 허용",
                example = "CONFIRMED", nullable = true)
        Certainty stageCertainty,
        @Schema(description = "성별. 생략 또는 null이면 UNKNOWN", example = "FEMALE", nullable = true)
        Sex sex,
        @Schema(description = "성별 정확성. 생략 또는 null이면 UNKNOWN", example = "ESTIMATED", nullable = true)
        Certainty sexCertainty,
        @Schema(description = "동일 사용자 소유 PET_PHOTO 파일 ID", example = "301", nullable = true)
        Long photoFileId
) {
    public CreatePetCommand toCommand() {
        return new CreatePetCommand(
                clientRequestId,
                speciesId,
                name,
                morphIds,
                customMorph,
                hatchedAt,
                lifeStageId,
                stageCertainty,
                sex,
                sexCertainty,
                photoFileId
        );
    }
}
