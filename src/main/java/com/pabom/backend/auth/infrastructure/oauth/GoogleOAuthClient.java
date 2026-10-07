package com.pabom.backend.auth.infrastructure.oauth;

import com.pabom.backend.auth.application.port.GoogleOAuthClientPort;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.exception.InvalidSocialUserInfoException;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import com.pabom.backend.auth.infrastructure.oauth.dto.GoogleTokenResponse;
import com.pabom.backend.auth.infrastructure.oauth.dto.GoogleUserInfoResponse;
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
public class GoogleOAuthClient implements GoogleOAuthClientPort {

    private static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String REQUIRED_SCOPES = "openid profile";
    private static final String CALLBACK_PATH = "/api/v1/auth/google/callback";

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties properties;

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
            GoogleUserInfoResponse userInfo = requestUserInfo(accessToken);
            if (userInfo == null || !StringUtils.hasText(userInfo.sub())) {
                throw new InvalidSocialUserInfoException("구글 회원 식별자가 없습니다.");
            }
            return SocialUserInfo.google(userInfo.sub(), userInfo.name());
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
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", properties.redirectUri());
        form.add("code", authorizationCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try {
            GoogleTokenResponse response = restTemplate.postForObject(
                    TOKEN_URL,
                    new HttpEntity<>(form, headers),
                    GoogleTokenResponse.class
            );
            if (response == null || !StringUtils.hasText(response.accessToken())) {
                throw new IllegalStateException("구글 토큰 응답에 Access Token이 없습니다.");
            }
            return response.accessToken();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("구글 토큰 교환에 실패했습니다.", exception);
        }
    }

    private GoogleUserInfoResponse requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        try {
            return restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    GoogleUserInfoResponse.class
            ).getBody();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("구글 사용자 정보 조회에 실패했습니다.", exception);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.clientId())
                || !StringUtils.hasText(properties.clientSecret())
                || !StringUtils.hasText(properties.redirectUri())
                || !properties.redirectUri().endsWith(CALLBACK_PATH)) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
