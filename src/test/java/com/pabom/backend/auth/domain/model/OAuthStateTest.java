package com.pabom.backend.auth.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuthStateTest {

    @Test
    void webStateHasNoAppRedirectUri() {
        OAuthState state = OAuthState.web();

        assertThat(state.value()).isNotBlank().doesNotContain(".");
        assertThat(OAuthState.parse(state.value()).clientType()).isEqualTo(OAuthClientType.WEB);
        assertThat(OAuthState.parse(state.value()).appRedirectUri()).isNull();
    }

    @Test
    void appStateRoundTripsAppRedirectUri() {
        OAuthState state = OAuthState.app("pabom-dev://auth/callback/google");

        OAuthState parsed = OAuthState.parse(state.value());

        assertThat(state.value()).matches("[A-Za-z0-9_.-]+");
        assertThat(parsed.clientType()).isEqualTo(OAuthClientType.APP);
        assertThat(parsed.appRedirectUri()).isEqualTo("pabom-dev://auth/callback/google");
        assertThat(parsed.value()).isEqualTo(state.value());
    }

    @Test
    void generatesDifferentValuesEachTime() {
        assertThat(OAuthState.app("pabom://auth/callback/kakao").value())
                .isNotEqualTo(OAuthState.app("pabom://auth/callback/kakao").value());
    }

    @Test
    void treatsMalformedValuesAsWeb() {
        assertThat(OAuthState.parse(null).clientType()).isEqualTo(OAuthClientType.WEB);
        assertThat(OAuthState.parse("plain-state").clientType()).isEqualTo(OAuthClientType.WEB);
        assertThat(OAuthState.parse(".abc").clientType()).isEqualTo(OAuthClientType.WEB);
        assertThat(OAuthState.parse("abc.").clientType()).isEqualTo(OAuthClientType.WEB);
        assertThat(OAuthState.parse("abc.!!!").clientType()).isEqualTo(OAuthClientType.WEB);
    }
}
