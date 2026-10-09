package com.pabom.backend.agreement.domain.service;

import com.pabom.backend.agreement.domain.type.AgreementType;
import org.springframework.stereotype.Component;

@Component
public class AgreementVersionPolicy {

    public String currentVersion(AgreementType type) {
        return switch (type) {
            case TERMS -> "1.0";
            case PRIVACY -> "1.0";
            case AGE_14 -> "1.0";
            case MARKETING -> "1.0";
        };
    }
}
