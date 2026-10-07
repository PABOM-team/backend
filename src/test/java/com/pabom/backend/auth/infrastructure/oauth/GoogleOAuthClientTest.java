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

class GoogleOAuthClientTest {

    private MockRestServiceServer server;
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id",
                "client-secret",
                "http://localhost:8080/api/v1/auth/google/callback"
        );
        googleOAuthClient = new GoogleOAuthClient(restTemplate, properties);
    }

    @Test
    void createsAuthorizationUrlWithRequiredScopes() {
        String authorizationUrl = googleOAuthClient.createAuthorizationUrl("state-value");

        assertThat(authorizationUrl)
                .contains("client_id=client-id")
                .contains("response_type=code")
                .contains("scope=openid%20profile")
                .doesNotContain("email")
                .contains("state=state-value");
    }

    @Test
    void exchangesCodeAndMapsGoogleUserInfo() {
        server.expect(once(), requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("code=authorization-code")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_secret=client-secret")))
                .andRespond(withSuccess("{\"access_token\":\"google-access-token\"}", MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer google-access-token"))
                .andRespond(withSuccess(
                        """
                        {
                          "sub": "123456",
                          "name": "파봄"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SocialUserInfo userInfo = googleOAuthClient.authenticate("authorization-code");

        assertThat(userInfo.provider().name()).isEqualTo("GOOGLE");
        assertThat(userInfo.providerId()).isEqualTo("123456");
        assertThat(userInfo.nickname()).isEqualTo("파봄");
        server.verify();
    }
}
