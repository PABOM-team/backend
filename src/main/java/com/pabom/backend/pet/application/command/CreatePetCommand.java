package com.pabom.backend.pet.application.command;

import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePetCommand(
        UUID clientRequestId,
        Long speciesId,
        String name,
        List<Long> morphIds,
        String customMorph,
        LocalDate hatchedAt,
        Long lifeStageId,
        Certainty stageCertainty,
        Sex sex,
        Certainty sexCertainty,
        Long photoFileId
) {
}
