package com.pabom.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.global.error.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class OAuthServiceResolverTest {

    @Test
    void resolvesServiceByProvider() {
        OAuthService googleService = mock(OAuthService.class);
        OAuthService kakaoService = mock(OAuthService.class);
        given(googleService.provider()).willReturn(OAuthProvider.GOOGLE);
        given(kakaoService.provider()).willReturn(OAuthProvider.KAKAO);
        OAuthServiceResolver resolver = new OAuthServiceResolver(List.of(googleService, kakaoService));

        assertThat(resolver.resolve("google")).isSameAs(googleService);
        assertThat(resolver.resolve("KAKAO")).isSameAs(kakaoService);

        assertThatThrownBy(() -> resolver.resolve("naver"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode()
                        .getCode()).isEqualTo("UNSUPPORTED_OAUTH_PROVIDER"));
    }
}
