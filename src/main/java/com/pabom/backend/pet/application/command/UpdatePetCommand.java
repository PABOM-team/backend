package com.pabom.backend.pet.application.command;

import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import java.time.LocalDate;
import java.util.List;

public record UpdatePetCommand(
        PatchValue<String> name,
        PatchValue<List<Long>> morphIds,
        PatchValue<String> customMorph,
        PatchValue<LocalDate> hatchedAt,
        PatchValue<LocalDate> adoptedAt,
        PatchValue<Long> lifeStageId,
        PatchValue<Certainty> stageCertainty,
        PatchValue<Sex> sex,
        PatchValue<Certainty> sexCertainty,
        PatchValue<Long> photoFileId
) {
    public UpdatePetCommand(
            PatchValue<String> name, PatchValue<List<Long>> morphIds,
            PatchValue<String> customMorph, PatchValue<LocalDate> hatchedAt,
            PatchValue<Long> lifeStageId, PatchValue<Certainty> stageCertainty,
            PatchValue<Sex> sex, PatchValue<Certainty> sexCertainty,
            PatchValue<Long> photoFileId
    ) {
        this(name, morphIds, customMorph, hatchedAt, PatchValue.absent(), lifeStageId,
                stageCertainty, sex, sexCertainty, photoFileId);
    }
}
