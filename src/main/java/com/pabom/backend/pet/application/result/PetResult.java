package com.pabom.backend.pet.application.result;

import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PetResult(
        Long id,
        Long speciesId,
        String name,
        List<MorphItem> morphs,
        String customMorph,
        LocalDate hatchedAt,
        LocalDate adoptedAt,
        Long lifeStageId,
        Certainty stageCertainty,
        Sex sex,
        Certainty sexCertainty,
        Long photoFileId,
        Instant createdAt,
        Instant updatedAt
) {
    public PetResult(
            Long id, Long speciesId, String name, List<MorphItem> morphs,
            String customMorph, LocalDate hatchedAt, Long lifeStageId,
            Certainty stageCertainty, Sex sex, Certainty sexCertainty,
            Long photoFileId, Instant createdAt, Instant updatedAt
    ) {
        this(id, speciesId, name, morphs, customMorph, hatchedAt, null, lifeStageId,
                stageCertainty, sex, sexCertainty, photoFileId, createdAt, updatedAt);
    }

    public static PetResult from(Pet pet) {
        return new PetResult(
                pet.getId(),
                pet.getSpeciesId(),
                pet.getName(),
                pet.getMorphs().stream()
                        .map(morph -> new MorphItem(morph.getId(), morph.getCode(), morph.getName()))
                        .toList(),
                pet.getCustomMorph(),
                pet.getHatchedAt(),
                pet.getAdoptedAt(),
                pet.getLifeStageId(),
                pet.getStageCertainty(),
                pet.getSex(),
                pet.getSexCertainty(),
                pet.getPhotoFileId(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }

    public record MorphItem(Long id, String code, String name) {
    }
}
