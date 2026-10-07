package com.pabom.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.domain.exception.InvalidSocialUserInfoException;
import org.junit.jupiter.api.Test;

class SocialUserInfoTest {

    @Test
    void rejectsKakaoProfileWithoutNickname() {
        assertThatThrownBy(() -> SocialUserInfo.kakao("123", null))
                .isInstanceOf(InvalidSocialUserInfoException.class)
                .hasMessageContaining("닉네임");
    }
}
