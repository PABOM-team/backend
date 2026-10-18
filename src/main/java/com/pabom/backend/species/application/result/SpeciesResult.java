package com.pabom.backend.species.application.result;

import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.domain.type.ReptileCategory;

public record SpeciesResult(
        Long id,
        String code,
        String commonName,
        ReptileCategory category,
        int displayOrder
) {
    public static SpeciesResult from(Species species) {
        return new SpeciesResult(
                species.getId(),
                species.getCode(),
                species.getCommonName(),
                species.getCategory(),
                species.getDisplayOrder()
        );
    }
}
