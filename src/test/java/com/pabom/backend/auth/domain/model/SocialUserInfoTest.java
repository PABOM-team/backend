package com.pabom.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.domain.exception.InvalidSocialUserInfoException;
import org.junit.jupiter.api.Test;

class SocialUserInfoTest {

    @Test
    void rejectsKakaoProfileWithoutNickname() {
        assertThatThrownBy(() -> SocialUserInfo.kakao("123", null, "pabom@example.com"))
                .isInstanceOf(InvalidSocialUserInfoException.class)
                .hasMessageContaining("닉네임");
    }

    @Test
    void rejectsKakaoProfileWithoutEmail() {
        assertThatThrownBy(() -> SocialUserInfo.kakao("123", "파봄", null))
                .isInstanceOf(InvalidSocialUserInfoException.class)
                .hasMessageContaining("이메일");
    }

    @Test
    void rejectsGoogleProfileWithoutEmail() {
        assertThatThrownBy(() -> SocialUserInfo.google("123", "파봄", null))
                .isInstanceOf(InvalidSocialUserInfoException.class)
                .hasMessageContaining("이메일");
    }
}
