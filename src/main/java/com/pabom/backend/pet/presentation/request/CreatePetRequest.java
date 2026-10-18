package com.pabom.backend.pet.presentation.request;

import com.pabom.backend.pet.application.command.CreatePetCommand;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreatePetRequest(
        @NotNull UUID clientRequestId,
        @NotNull Long speciesId,
        @NotBlank @Size(max = 20) String name,
        List<@NotNull Long> morphIds,
        @Size(max = 40) String customMorph,
        @PastOrPresent LocalDate hatchedAt,
        Long lifeStageId,
        Certainty stageCertainty,
        Sex sex,
        Certainty sexCertainty,
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
