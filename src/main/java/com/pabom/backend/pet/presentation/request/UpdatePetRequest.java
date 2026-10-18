package com.pabom.backend.pet.presentation.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.pabom.backend.pet.application.command.PatchValue;
import com.pabom.backend.pet.application.command.UpdatePetCommand;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class UpdatePetRequest {

    @Size(max = 20)
    private String name;
    private List<@NotNull Long> morphIds;
    @Size(max = 40)
    private String customMorph;
    @PastOrPresent
    private LocalDate hatchedAt;
    private Long lifeStageId;
    private Certainty stageCertainty;
    private Sex sex;
    private Certainty sexCertainty;
    private Long photoFileId;

    private boolean namePresent;
    private boolean morphIdsPresent;
    private boolean customMorphPresent;
    private boolean hatchedAtPresent;
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
    public void setCustomMorph(String customMorph) { this.customMorph = customMorph; this.customMorphPresent = true; }
    @JsonSetter("hatchedAt")
    public void setHatchedAt(LocalDate hatchedAt) { this.hatchedAt = hatchedAt; this.hatchedAtPresent = true; }
    @JsonSetter("lifeStageId")
    public void setLifeStageId(Long lifeStageId) { this.lifeStageId = lifeStageId; this.lifeStageIdPresent = true; }
    @JsonSetter("stageCertainty")
    public void setStageCertainty(Certainty value) { this.stageCertainty = value; this.stageCertaintyPresent = true; }
    @JsonSetter("sex")
    public void setSex(Sex sex) { this.sex = sex; this.sexPresent = true; }
    @JsonSetter("sexCertainty")
    public void setSexCertainty(Certainty value) { this.sexCertainty = value; this.sexCertaintyPresent = true; }
    @JsonSetter("photoFileId")
    public void setPhotoFileId(Long photoFileId) { this.photoFileId = photoFileId; this.photoFileIdPresent = true; }

    @JsonIgnore
    public UpdatePetCommand toCommand() {
        return new UpdatePetCommand(
                field(namePresent, name),
                field(morphIdsPresent, morphIds),
                field(customMorphPresent, customMorph),
                field(hatchedAtPresent, hatchedAt),
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
