package com.pabom.backend.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSwaggerTest {

    private static final String TOKEN_LOGIN = "$.paths['/api/v1/auth/{provider}/token'].post";

    private static final String LOGIN_URL = "$.paths['/api/v1/auth/{provider}/login-url'].get";
    private static final String APP_CALLBACK = "$.paths['/api/v1/auth/{provider}/app-callback'].get";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsAppRedirectParameterAndAppCallback() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(LOGIN_URL + ".parameters[?(@.name == 'redirect_uri')].in").value("query"))
                .andExpect(jsonPath(LOGIN_URL + ".responses['400'].content['application/json'].examples"
                        + "['허용되지 않은 앱 Redirect URI'].value.error.code").value("REDIRECT_URI_NOT_ALLOWED"))
                .andExpect(jsonPath(APP_CALLBACK + ".tags[0]").value("Auth"))
                .andExpect(jsonPath(APP_CALLBACK + ".parameters[?(@.name == 'error_description')].in").value("query"))
                .andExpect(jsonPath(APP_CALLBACK + ".responses['302'].headers.Location").exists())
                .andExpect(jsonPath(APP_CALLBACK + ".responses['400'].content['application/json'].examples"
                        + ".REDIRECT_URI_NOT_ALLOWED.value.error.code").value("REDIRECT_URI_NOT_ALLOWED"))
                .andExpect(jsonPath(APP_CALLBACK + ".responses['400'].content['application/json'].examples"
                        + ".UNSUPPORTED_OAUTH_PROVIDER.value.error.code").value("UNSUPPORTED_OAUTH_PROVIDER"));
    }

    @Test
    void documentsSocialTokenLoginEnvelopeRequestAndAllErrorCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOKEN_LOGIN + ".tags[0]").value("Auth"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".security").doesNotExist())
                .andExpect(jsonPath(TOKEN_LOGIN + ".parameters[0].name").value("provider"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".parameters[0].in").value("path"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".requestBody.required").value(true))
                .andExpect(jsonPath(TOKEN_LOGIN + ".requestBody.content['application/json'].schema.$ref")
                        .value("#/components/schemas/SocialTokenLoginRequest"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".requestBody.content['application/json'].examples.kakao.value.accessToken")
                        .exists())
                .andExpect(jsonPath(TOKEN_LOGIN + ".requestBody.content['application/json'].examples.google.value.idToken")
                        .exists())
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/OAuthLoginApiResponse"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['200'].headers['Set-Cookie']").exists())
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['200'].content['application/json'].examples.SIGNUP.value.resultType")
                        .value("SUCCESS"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['200'].content['application/json'].examples.SIGNUP.value.success.data.signupToken")
                        .exists())
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['200'].content['application/json'].examples.ACCESS.value.success.data.accessToken")
                        .exists())
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['400'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/ErrorApiResponse"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['400'].content['application/json'].examples.VALIDATION_FAILED.value.error.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['400'].content['application/json'].examples.UNSUPPORTED_OAUTH_PROVIDER.value.error.code")
                        .value("UNSUPPORTED_OAUTH_PROVIDER"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['401'].content['application/json'].examples.INVALID_SOCIAL_TOKEN.value.error.code")
                        .value("INVALID_SOCIAL_TOKEN"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['502'].content['application/json'].examples.KAKAO_UNAVAILABLE.value.error.code")
                        .value("KAKAO_UNAVAILABLE"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['502'].content['application/json'].examples.GOOGLE_UNAVAILABLE.value.error.code")
                        .value("GOOGLE_UNAVAILABLE"))
                .andExpect(jsonPath(TOKEN_LOGIN + ".responses['500']").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.SocialTokenLoginRequest.properties.accessToken").exists())
                .andExpect(jsonPath("$.components.schemas.SocialTokenLoginRequest.properties.idToken").exists());
    }
}
