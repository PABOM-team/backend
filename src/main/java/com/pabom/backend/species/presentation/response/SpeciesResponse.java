package com.pabom.backend.species.presentation.response;

import com.pabom.backend.species.application.result.SpeciesResult;
import com.pabom.backend.species.domain.type.ReptileCategory;

public record SpeciesResponse(
        Long id,
        String code,
        String commonName,
        ReptileCategory category,
        int displayOrder
) {
    public static SpeciesResponse from(SpeciesResult result) {
        return new SpeciesResponse(
                result.id(), result.code(), result.commonName(), result.category(), result.displayOrder()
        );
    }
}
