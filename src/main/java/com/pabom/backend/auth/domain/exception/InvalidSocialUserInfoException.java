package com.pabom.backend.auth.domain.exception;

public class InvalidSocialUserInfoException extends RuntimeException {

    public InvalidSocialUserInfoException(String message) {
        super(message);
    }
}
