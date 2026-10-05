package com.pabom.backend.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.pabom.backend.auth.domain.model.SocialUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class KakaoOAuthClientTest {

    private MockRestServiceServer server;
    private KakaoOAuthClient kakaoOAuthClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        KakaoOAuthProperties properties = new KakaoOAuthProperties(
                "client-id",
                "client-secret",
                "http://localhost:8080/api/v1/auth/kakao/callback"
        );
        kakaoOAuthClient = new KakaoOAuthClient(restTemplate, properties);
    }

    @Test
    void createsAuthorizationUrlWithRequiredScopes() {
        String authorizationUrl = kakaoOAuthClient.createAuthorizationUrl("state-value");

        assertThat(authorizationUrl)
                .contains("client_id=client-id")
                .contains("response_type=code")
                .contains("scope=profile_nickname,account_email")
                .contains("state=state-value");
    }

    @Test
    void exchangesCodeAndMapsKakaoUserInfo() {
        server.expect(once(), requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=authorization-code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=client-secret")))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer kakao-access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 123456,
                          "kakao_account": {
                            "email": "pabom@example.com",
                            "profile": { "nickname": "파봄" }
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SocialUserInfo userInfo = kakaoOAuthClient.authenticate("authorization-code");

        assertThat(userInfo.providerId()).isEqualTo("123456");
        assertThat(userInfo.nickname()).isEqualTo("파봄");
        assertThat(userInfo.email()).isEqualTo("pabom@example.com");
        server.verify();
    }
}
