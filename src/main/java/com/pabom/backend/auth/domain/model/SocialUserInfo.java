package com.pabom.backend.auth.domain.model;

import com.pabom.backend.auth.domain.exception.InvalidSocialUserInfoException;

public record SocialUserInfo(
        OAuthProvider provider,
        String providerId,
        String nickname,
        String email
) {

    public SocialUserInfo {
        if (provider == null) {
            throw new InvalidSocialUserInfoException("소셜 로그인 제공자 정보가 없습니다.");
        }
        if (providerId == null || providerId.isBlank()) {
            throw new InvalidSocialUserInfoException("소셜 로그인 회원 식별자가 없습니다.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new InvalidSocialUserInfoException("소셜 로그인 닉네임이 없습니다. 동의 항목을 확인해 주세요.");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidSocialUserInfoException("소셜 로그인 이메일이 없습니다. 동의 항목을 확인해 주세요.");
        }
    }

    public static SocialUserInfo kakao(String providerId, String nickname, String email) {
        return new SocialUserInfo(OAuthProvider.KAKAO, providerId, nickname, email);
    }

    public static SocialUserInfo google(String providerId, String nickname, String email) {
        return new SocialUserInfo(OAuthProvider.GOOGLE, providerId, nickname, email);
    }
}
