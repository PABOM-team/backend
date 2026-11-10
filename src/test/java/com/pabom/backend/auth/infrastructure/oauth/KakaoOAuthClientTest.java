package com.pabom.backend.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class KakaoOAuthClientTest {

    private static final String REDIRECT_URI =
            "https://dev.pabom.app/api/v1/auth/kakao/callback";

    private MockRestServiceServer server;
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        KakaoOAuthProperties properties = new KakaoOAuthProperties(
                "client-id",
                "123456789",
                "client-secret",
                REDIRECT_URI
        );
        kakaoOAuthClient = new KakaoOAuthClient(restTemplate, properties);
    }

    @Test
    void createsAuthorizationUrlWithRequiredScopes() {
        String authorizationUrl = kakaoOAuthClient.createAuthorizationUrl("state-value");

        assertThat(authorizationUrl)
                .contains("client_id=client-id")
                .contains("redirect_uri=https://dev.pabom.app/api/v1/auth/kakao/callback")
                .contains("response_type=code")
                .contains("scope=profile_nickname")
                .doesNotContain("account_email")
                .contains("state=state-value");
    }

    @Test
    void exchangesCodeAndMapsKakaoUserInfo() {
        server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=authorization-code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=client-secret")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "redirect_uri=https%3A%2F%2Fdev.pabom.app%2Fapi%2Fv1%2Fauth%2Fkakao%2Fcallback")))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 123456,
                          "kakao_account": {
                            "profile": { "nickname": "파봄" }
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SocialUserInfo userInfo = kakaoOAuthClient.authenticate("authorization-code");

        assertThat(userInfo.providerId()).isEqualTo("123456");
        assertThat(userInfo.nickname()).isEqualTo("파봄");
        server.verify();
    }

    @Test
    void mapsKakaoClientErrorToInvalidCode() {
        server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> kakaoOAuthClient.authenticate("invalid-code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("KAKAO_CODE_INVALID"));
    }

    @Test
    void mapsKakaoServerErrorToUnavailable() {
        server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> kakaoOAuthClient.authenticate("authorization-code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("KAKAO_UNAVAILABLE"));
    }

    @Test
    void authenticatesWithAccessTokenWhenAppIdMatches() {
        server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sdk-access-token"))
                .andRespond(withSuccess(
                        "{\"id\":123456,\"expires_in\":7199,\"app_id\":123456789}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(once(), requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer sdk-access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 123456,
                          "kakao_account": {
                            "profile": { "nickname": "파봄" }
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SocialUserInfo userInfo = kakaoOAuthClient.authenticateWithAccessToken("sdk-access-token");

        assertThat(userInfo.provider().name()).isEqualTo("KAKAO");
        assertThat(userInfo.providerId()).isEqualTo("123456");
        assertThat(userInfo.nickname()).isEqualTo("파봄");
        server.verify();
    }

    @Test
    void rejectsAccessTokenIssuedForAnotherApp() {
        server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withSuccess(
                        "{\"id\":123456,\"expires_in\":7199,\"app_id\":999999}",
                        MediaType.APPLICATION_JSON
                ));
        server.expect(never(), requestTo("https://kapi.kakao.com/v2/user/me"));

        assertThatThrownBy(() -> kakaoOAuthClient.authenticateWithAccessToken("foreign-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("INVALID_SOCIAL_TOKEN"));
        server.verify();
    }

    @Test
    void mapsExpiredAccessTokenToInvalidSocialToken() {
        server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> kakaoOAuthClient.authenticateWithAccessToken("expired-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("INVALID_SOCIAL_TOKEN"));
    }

    @Test
    void mapsAccessTokenInfoServerErrorToUnavailable() {
        server.expect(once(), requestTo("https://kapi.kakao.com/v1/user/access_token_info"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> kakaoOAuthClient.authenticateWithAccessToken("sdk-access-token"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("KAKAO_UNAVAILABLE"));
    }
}
