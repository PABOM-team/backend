package com.pabom.backend.agreement.presentation.request;

import com.pabom.backend.agreement.domain.type.AgreementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AgreementCompletionRequest(
        @NotEmpty List<@NotNull @Valid AgreementItem> agreements
) {

    public record AgreementItem(
            @NotNull AgreementType type,
            @NotBlank String version,
            @NotNull Boolean agreed
    ) {
    }
}
