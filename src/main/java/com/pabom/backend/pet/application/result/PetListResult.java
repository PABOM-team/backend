package com.pabom.backend.pet.application.result;

import com.pabom.backend.pet.domain.entity.Pet;
import java.util.List;

public record PetListResult(
        Long id,
        String name,
        Long speciesId,
        String speciesName,
        List<PetResult.MorphItem> morphs,
        String customMorph,
        Long lifeStageId,
        String lifeStageName,
        Long photoFileId
) {
    public static PetListResult from(Pet pet, String speciesName, String lifeStageName) {
        return new PetListResult(
                pet.getId(),
                pet.getName(),
                pet.getSpeciesId(),
                speciesName,
                pet.getMorphs().stream()
                        .map(morph -> new PetResult.MorphItem(morph.getId(), morph.getCode(), morph.getName()))
                        .toList(),
                pet.getCustomMorph(),
                pet.getLifeStageId(),
                lifeStageName,
                pet.getPhotoFileId()
        );
    }
}
