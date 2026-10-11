package com.pabom.backend.auth.domain.exception;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;

public class RefreshTokenReuseException extends BusinessException {

    public RefreshTokenReuseException() {
        super(AuthErrorCode.REFRESH_REUSED);
    }
}
