package com.pabom.backend.agreement.application.command;

import com.pabom.backend.agreement.domain.type.AgreementType;
import java.util.List;

public record CompleteAgreementsCommand(List<Agreement> agreements) {

    public CompleteAgreementsCommand {
        agreements = agreements == null ? List.of() : List.copyOf(agreements);
    }

    public record Agreement(AgreementType type, String version, boolean agreed) {
    }
}
