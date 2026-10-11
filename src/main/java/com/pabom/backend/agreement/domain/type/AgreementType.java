package com.pabom.backend.agreement.domain.type;

public enum AgreementType {
    TERMS(true),
    PRIVACY(true),
    AGE_14(true),
    MARKETING(false);

    private final boolean required;

    AgreementType(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }
}
