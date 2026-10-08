package com.pabom.backend.auth.application.result;

import com.pabom.backend.user.domain.aggregate.User;

public record OAuthLoginCompletionResult(
        LoginTokenType tokenType,
        String token,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn,
        boolean isNewUser,
        User user
) {

    public static OAuthLoginCompletionResult signup(
            String signupToken,
            long expiresIn,
            boolean isNewUser,
            User user
    ) {
        return new OAuthLoginCompletionResult(
                LoginTokenType.SIGNUP,
                signupToken,
                expiresIn,
                null,
                0,
                isNewUser,
                user
        );
    }

    public static OAuthLoginCompletionResult access(
            String accessToken,
            long expiresIn,
            String refreshToken,
            long refreshExpiresIn,
            User user
    ) {
        return new OAuthLoginCompletionResult(
                LoginTokenType.ACCESS,
                accessToken,
                expiresIn,
                refreshToken,
                refreshExpiresIn,
                false,
                user
        );
    }

    public enum LoginTokenType {
        SIGNUP,
        ACCESS
    }
}
