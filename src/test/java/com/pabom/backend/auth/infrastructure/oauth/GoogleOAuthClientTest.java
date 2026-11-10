package com.pabom.backend.auth.infrastructure.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pabom.backend.global.error.BusinessException;
import java.time.Instant;
import java.util.Date;
import com.pabom.backend.auth.domain.model.SocialUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class GoogleOAuthClientTest {

    private static final String REDIRECT_URI =
            "https://dev.pabom.app/api/v1/auth/google/callback";
    private static final String JWK_SET_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final RSAKey GOOGLE_SIGNING_KEY = generateKey("google-key");
    private static final RSAKey FOREIGN_SIGNING_KEY = generateKey("google-key");

    private MockRestServiceServer server;
    private GoogleOAuthClient googleOAuthClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        GoogleOAuthProperties properties = new GoogleOAuthProperties(
                "client-id",
                "client-secret",
                REDIRECT_URI
        );
        googleOAuthClient = new GoogleOAuthClient(restTemplate, properties);
    }

    @Test
    void createsAuthorizationUrlWithRequiredScopes() {
        String authorizationUrl = googleOAuthClient.createAuthorizationUrl("state-value");

        assertThat(authorizationUrl)
                .contains("client_id=client-id")
                .contains("redirect_uri=https://dev.pabom.app/api/v1/auth/google/callback")
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "redirect_uri=https%3A%2F%2Fdev.pabom.app%2Fapi%2Fv1%2Fauth%2Fgoogle%2Fcallback")))
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

    @Test
    void mapsGoogleClientErrorToInvalidCode() {
        server.expect(once(), requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> googleOAuthClient.authenticate("invalid-code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("GOOGLE_CODE_INVALID"));
    }

    @Test
    void mapsGoogleServerErrorToUnavailable() {
        server.expect(once(), requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> googleOAuthClient.authenticate("authorization-code"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("GOOGLE_UNAVAILABLE"));
    }

    @Test
    void verifiesIdTokenAndMapsSameSubjectAndNameAsCodeFlow() throws JOSEException {
        expectGoogleJwkSet();
        String idToken = signIdToken(GOOGLE_SIGNING_KEY, "https://accounts.google.com", "client-id",
                Instant.now().plusSeconds(600));

        SocialUserInfo userInfo = googleOAuthClient.authenticateWithIdToken(idToken);

        assertThat(userInfo.provider().name()).isEqualTo("GOOGLE");
        assertThat(userInfo.providerId()).isEqualTo("123456");
        assertThat(userInfo.nickname()).isEqualTo("파봄");
    }

    @Test
    void acceptsIssuerWithoutScheme() throws JOSEException {
        expectGoogleJwkSet();
        String idToken = signIdToken(GOOGLE_SIGNING_KEY, "accounts.google.com", "client-id",
                Instant.now().plusSeconds(600));

        assertThat(googleOAuthClient.authenticateWithIdToken(idToken).providerId()).isEqualTo("123456");
    }

    @Test
    void rejectsIdTokenIssuedForAnotherAudience() throws JOSEException {
        expectGoogleJwkSet();
        String idToken = signIdToken(GOOGLE_SIGNING_KEY, "https://accounts.google.com", "other-client-id",
                Instant.now().plusSeconds(600));

        assertInvalidSocialToken(idToken);
    }

    @Test
    void rejectsIdTokenFromAnotherIssuer() throws JOSEException {
        expectGoogleJwkSet();
        String idToken = signIdToken(GOOGLE_SIGNING_KEY, "https://evil.example.com", "client-id",
                Instant.now().plusSeconds(600));

        assertInvalidSocialToken(idToken);
    }

    @Test
    void rejectsExpiredIdToken() throws JOSEException {
        expectGoogleJwkSet();
        String idToken = signIdToken(GOOGLE_SIGNING_KEY, "https://accounts.google.com", "client-id",
                Instant.now().minusSeconds(600));

        assertInvalidSocialToken(idToken);
    }

    @Test
    void rejectsIdTokenSignedWithUnknownKey() throws JOSEException {
        expectGoogleJwkSet();
        String idToken = signIdToken(FOREIGN_SIGNING_KEY, "https://accounts.google.com", "client-id",
                Instant.now().plusSeconds(600));

        assertInvalidSocialToken(idToken);
    }

    @Test
    void rejectsMalformedIdToken() {
        assertInvalidSocialToken("not-a-jwt");
    }

    private void assertInvalidSocialToken(String idToken) {
        assertThatThrownBy(() -> googleOAuthClient.authenticateWithIdToken(idToken))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception)
                        .errorCode().getCode()).isEqualTo("INVALID_SOCIAL_TOKEN"));
    }

    private void expectGoogleJwkSet() {
        server.expect(manyTimes(), requestTo(JWK_SET_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        new JWKSet(GOOGLE_SIGNING_KEY.toPublicJWK()).toString(),
                        MediaType.APPLICATION_JSON
                ));
    }

    private static String signIdToken(
            RSAKey signingKey,
            String issuer,
            String audience,
            Instant expiresAt
    ) throws JOSEException {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("123456")
                .claim("name", "파봄")
                .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                .expirationTime(Date.from(expiresAt))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(),
                claims
        );
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
