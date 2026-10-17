package com.pabom.backend.file.domain.type;

public enum FilePurpose {
    PET_PHOTO,
    RECORD_PHOTO,
    DEATH_EVIDENCE,
    REPORT_RECEIPT;

    public boolean isUploadSupported() {
        return this == PET_PHOTO || this == RECORD_PHOTO;
    }
}
