package com.pabom.backend.auth.infrastructure.oauth;

import com.pabom.backend.auth.application.port.KakaoOAuthClientPort;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.exception.InvalidSocialUserInfoException;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.auth.infrastructure.oauth.dto.KakaoTokenResponse;
import com.pabom.backend.auth.infrastructure.oauth.dto.KakaoUserInfoResponse;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements KakaoOAuthClientPort {

    private static final String AUTHORIZATION_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String REQUIRED_SCOPES = "profile_nickname,account_email";
    private static final String CALLBACK_PATH = "/api/v1/auth/kakao/callback";

    private final RestTemplate restTemplate;
    private final KakaoOAuthProperties properties;

    @Override
    public String createAuthorizationUrl(String state) {
        validateConfiguration();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", REQUIRED_SCOPES)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public SocialUserInfo authenticate(String authorizationCode) {
        validateConfiguration();
        try {
            String accessToken = exchangeCode(authorizationCode);
            KakaoUserInfoResponse userInfo = requestUserInfo(accessToken);
            if (userInfo == null || userInfo.id() == null) {
                throw new InvalidSocialUserInfoException("카카오 회원번호가 없습니다.");
            }
            return SocialUserInfo.kakao(
                    userInfo.id().toString(),
                    userInfo.nickname(),
                    userInfo.email()
            );
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(AuthErrorCode.INVALID_SOCIAL_TOKEN, exception);
        }
    }

    private String exchangeCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", authorizationCode);
        if (StringUtils.hasText(properties.clientSecret())) {
            form.add("client_secret", properties.clientSecret());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            KakaoTokenResponse response = restTemplate.postForObject(
                    TOKEN_URL,
                    new HttpEntity<>(form, headers),
                    KakaoTokenResponse.class
            );
            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new IllegalStateException("카카오 토큰 응답에 Access Token이 없습니다.");
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("카카오 토큰 교환에 실패했습니다.", exception);
        }
    }

    private KakaoUserInfoResponse requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            return restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoUserInfoResponse.class
            ).getBody();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("카카오 사용자 정보 조회에 실패했습니다.", exception);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.clientId())
                || !StringUtils.hasText(properties.redirectUri())
                || !properties.redirectUri().endsWith(CALLBACK_PATH)) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
