package com.pabom.backend.auth.domain.model;

import com.pabom.backend.auth.domain.exception.InvalidSocialUserInfoException;

public record SocialUserInfo(
        OAuthProvider provider,
        String providerId,
        String nickname
) {

    public SocialUserInfo {
        if (provider == null) {
            throw new InvalidSocialUserInfoException("소셜 로그인 제공자 정보가 없습니다.");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new InvalidSocialUserInfoException("소셜 로그인 회원 식별자가 없습니다.");
        }
    }

    public static SocialUserInfo kakao(String providerId, String nickname) {
        return new SocialUserInfo(OAuthProvider.KAKAO, providerId, nickname);
    }

    public static SocialUserInfo google(String providerId, String nickname) {
        return new SocialUserInfo(OAuthProvider.GOOGLE, providerId, nickname);
    }
}
