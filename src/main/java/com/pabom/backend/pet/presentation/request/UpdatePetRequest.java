package com.pabom.backend.pet.presentation.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.pabom.backend.pet.application.command.PatchValue;
import com.pabom.backend.pet.application.command.UpdatePetCommand;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "개체 수정 요청. 생략한 필드는 유지하며, nullable 필드의 명시적 null은 값을 삭제합니다.")
public class UpdatePetRequest {

    @Schema(description = "개체 이름. 생략하면 유지하며 null은 허용하지 않음", example = "콩콩이", minLength = 1, maxLength = 20)
    @Size(max = 20)
    private String name;
    @Schema(description = "전체 교체할 모프 ID 목록. 생략하면 유지, 빈 배열이면 모두 해제, null은 허용하지 않음",
            example = "[3]")
    private List<@NotNull Long> morphIds;
    @Schema(description = "기타 모프. 생략하면 유지, null이면 삭제", example = "트라이컬러", maxLength = 40, nullable = true)
    @Size(max = 40)
    private String customMorph;
    @Schema(description = "부화일. 생략하면 유지, null이면 모름", example = "2026-03-02",
            type = "string", format = "date", nullable = true)
    @PastOrPresent
    private LocalDate hatchedAt;
    @Schema(description = "데려온 날. 생략하면 유지, 명시적 null이면 삭제", example = "2026-04-10",
            type = "string", format = "date", nullable = true)
    @PastOrPresent
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate adoptedAt;
    @Schema(description = "성장 단계 ID. 생략하면 유지, null이면 해제. 최종 정확성이 CONFIRMED/ESTIMATED이면 필수",
            example = "1", nullable = true)
    private Long lifeStageId;
    @Schema(description = "성장 단계 정확성. 생략하면 유지. UNKNOWN이면 최종 lifeStageId는 null이어야 함",
            example = "CONFIRMED")
    private Certainty stageCertainty;
    @Schema(description = "성별. 생략하면 유지", example = "UNKNOWN")
    private Sex sex;
    @Schema(description = "성별 정확성. 생략하면 유지", example = "UNKNOWN")
    private Certainty sexCertainty;
    @Schema(description = "사진 파일 ID. 생략하면 유지, null이면 사진 제거", example = "301", nullable = true)
    private Long photoFileId;

    private boolean namePresent;
    private boolean morphIdsPresent;
    private boolean customMorphPresent;
    private boolean hatchedAtPresent;
    private boolean adoptedAtPresent;
    private boolean lifeStageIdPresent;
    private boolean stageCertaintyPresent;
    private boolean sexPresent;
    private boolean sexCertaintyPresent;
    private boolean photoFileIdPresent;

    @JsonSetter("name")
    public void setName(String name) { this.name = name; this.namePresent = true; }
    @JsonSetter("morphIds")
    public void setMorphIds(List<Long> morphIds) { this.morphIds = morphIds; this.morphIdsPresent = true; }
    @JsonSetter("customMorph")
    @Schema(description = "기타 모프. 명시적 null이면 삭제", nullable = true)
    public void setCustomMorph(String customMorph) { this.customMorph = customMorph; this.customMorphPresent = true; }
    @JsonSetter("hatchedAt")
    @Schema(description = "부화일. 명시적 null이면 모름", nullable = true)
    public void setHatchedAt(LocalDate hatchedAt) { this.hatchedAt = hatchedAt; this.hatchedAtPresent = true; }
    @JsonSetter("adoptedAt")
    @Schema(description = "데려온 날. 명시적 null이면 삭제", nullable = true)
    public void setAdoptedAt(LocalDate adoptedAt) { this.adoptedAt = adoptedAt; this.adoptedAtPresent = true; }
    @JsonSetter("lifeStageId")
    @Schema(description = "성장 단계 ID. 명시적 null이면 해제하며 최종 정확성과 함께 검증", nullable = true)
    public void setLifeStageId(Long lifeStageId) { this.lifeStageId = lifeStageId; this.lifeStageIdPresent = true; }
    @JsonSetter("stageCertainty")
    public void setStageCertainty(Certainty value) { this.stageCertainty = value; this.stageCertaintyPresent = true; }
    @JsonSetter("sex")
    public void setSex(Sex sex) { this.sex = sex; this.sexPresent = true; }
    @JsonSetter("sexCertainty")
    public void setSexCertainty(Certainty value) { this.sexCertainty = value; this.sexCertaintyPresent = true; }
    @JsonSetter("photoFileId")
    @Schema(description = "사진 파일 ID. 명시적 null이면 사진 제거", nullable = true)
    public void setPhotoFileId(Long photoFileId) { this.photoFileId = photoFileId; this.photoFileIdPresent = true; }

    @JsonIgnore
    public UpdatePetCommand toCommand() {
        return new UpdatePetCommand(
                field(namePresent, name),
                field(morphIdsPresent, morphIds),
                field(customMorphPresent, customMorph),
                field(hatchedAtPresent, hatchedAt),
                field(adoptedAtPresent, adoptedAt),
                field(lifeStageIdPresent, lifeStageId),
                field(stageCertaintyPresent, stageCertainty),
                field(sexPresent, sex),
                field(sexCertaintyPresent, sexCertainty),
                field(photoFileIdPresent, photoFileId)
        );
    }

    private <T> PatchValue<T> field(boolean present, T value) {
        return present ? PatchValue.of(value) : PatchValue.absent();
    }
}
