package com.pabom.backend.pet.presentation.response;

import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PetResponse(
        Long id,
        Long speciesId,
        String name,
        List<MorphResponse> morphs,
        String customMorph,
        LocalDate hatchedAt,
        Long lifeStageId,
        Certainty stageCertainty,
        Sex sex,
        Certainty sexCertainty,
        Long photoFileId,
        Instant createdAt,
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

    public record MorphResponse(Long id, String code, String name) {
    }
}
