package com.pabom.backend.species.application.result;

import com.pabom.backend.species.domain.entity.Morph;

public record MorphResult(Long id, String code, String name, int displayOrder) {

    public static MorphResult from(Morph morph) {
        return new MorphResult(morph.getId(), morph.getCode(), morph.getName(), morph.getDisplayOrder());
    }
}
