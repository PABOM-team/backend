package com.pabom.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SocialUserInfoTest {

    @Test
    void allowsKakaoProfileWithoutNicknameForTemporaryNicknameFallback() {
        SocialUserInfo userInfo = SocialUserInfo.kakao("123", null);

        assertThat(userInfo.nickname()).isNull();
    }
}
