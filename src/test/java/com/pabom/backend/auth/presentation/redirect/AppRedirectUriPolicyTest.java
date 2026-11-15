package com.pabom.backend.auth.presentation.redirect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.config.AuthAppRedirectProperties;
import com.pabom.backend.global.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppRedirectUriPolicyTest {

    private final AppRedirectUriPolicy policy = new AppRedirectUriPolicy(new AuthAppRedirectProperties(List.of(
            "pabom://auth/callback/{provider}",
            " pabom-dev://auth/callback/{provider} "
    )));

    @Test
    void allowsExactAppUrisPerProvider() {
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "pabom://auth/callback/kakao")).isTrue();
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "pabom-dev://auth/callback/kakao")).isTrue();
        assertThat(policy.isAllowed(OAuthProvider.GOOGLE, "pabom://auth/callback/google")).isTrue();
        assertThat(policy.isAllowed(OAuthProvider.GOOGLE, "pabom-dev://auth/callback/google")).isTrue();
    }

    @Test
    void rejectsAnythingElse() {
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, null)).isFalse();
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "pabom://auth/callback/google")).isFalse();
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "pabom://auth/callback/{provider}")).isFalse();
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "pabom://auth/callback/kakao?x=1")).isFalse();
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "PABOM://auth/callback/kakao")).isFalse();
        assertThat(policy.isAllowed(OAuthProvider.KAKAO, "exp://127.0.0.1:8081/--/auth/callback/kakao")).isFalse();

        assertThatThrownBy(() -> policy.validate(OAuthProvider.KAKAO, "evil://auth/callback/kakao"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("REDIRECT_URI_NOT_ALLOWED"));
    }

    @Test
    void rejectsEverythingWhenAllowlistIsEmpty() {
        AppRedirectUriPolicy emptyPolicy = new AppRedirectUriPolicy(new AuthAppRedirectProperties(null));

        assertThat(emptyPolicy.isAllowed(OAuthProvider.KAKAO, "pabom://auth/callback/kakao")).isFalse();
    }
}
