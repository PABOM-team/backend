package com.pabom.backend.global.error;

public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final Object details;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(BaseErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(BaseErrorCode errorCode, Object details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public BaseErrorCode errorCode() {
        return errorCode;
    }

    public Object details() {
        return details;
    }
}
