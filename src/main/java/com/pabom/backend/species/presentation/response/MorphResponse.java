package com.pabom.backend.species.presentation.response;

import com.pabom.backend.species.application.result.MorphResult;

public record MorphResponse(Long id, String code, String name, int displayOrder) {

    public static MorphResponse from(MorphResult result) {
        return new MorphResponse(result.id(), result.code(), result.name(), result.displayOrder());
    }
}
