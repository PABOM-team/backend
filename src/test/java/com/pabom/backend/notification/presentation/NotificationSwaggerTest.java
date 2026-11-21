package com.pabom.backend.notification.presentation;

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
class NotificationSwaggerTest {

    private static final String TOKENS = "$.paths['/api/v1/push-tokens']";
    private static final String TOKEN = "$.paths['/api/v1/push-tokens/{token}']";
    private static final String SETTINGS = "$.paths['/api/v1/users/me/notification-settings']";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsPushTokenApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(TOKENS + ".put.tags[0]").value("Notification"))
                .andExpect(jsonPath(TOKENS + ".put.security[0].Authorization").isArray())
                .andExpect(jsonPath(TOKENS + ".put.requestBody.required").value(true))
                .andExpect(jsonPath(TOKENS + ".put.responses['204']").exists())
                .andExpect(jsonPath(TOKENS + ".put.responses['400'].content['application/json'].examples.VALIDATION_FAILED.value.error.code")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath(TOKENS + ".put.responses['400'].content['application/json'].examples.INVALID_PUSH_TOKEN.value.error.code")
                        .value("INVALID_PUSH_TOKEN"))
                .andExpect(jsonPath(TOKENS + ".put.responses['401'].content['application/json'].examples.USER_WITHDRAWN.value.error.code")
                        .value("USER_WITHDRAWN"))
                .andExpect(jsonPath(TOKEN + ".delete.responses['204']").exists())
                .andExpect(jsonPath(TOKEN + ".delete.responses['401'].content['application/json'].examples.AUTH_REQUIRED.value.error.code")
                        .value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.components.schemas.RegisterPushTokenRequest.properties.platform.enum")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("IOS", "ANDROID")))
                .andExpect(jsonPath("$.components.schemas.RegisterPushTokenRequest.properties.appVariant.enum")
                        .value(org.hamcrest.Matchers.containsInAnyOrder("PRODUCTION", "DEVELOPMENT")));
    }

    @Test
    void documentsNotificationSettingApis() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(SETTINGS + ".get.tags[0]").value("Notification"))
                .andExpect(jsonPath(SETTINGS + ".get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/NotificationSettingApiResponse"))
                .andExpect(jsonPath(SETTINGS + ".get.responses['200'].content['application/json'].examples['기본값'].value.success.data.careReminderTime")
                        .value("09:00"))
                .andExpect(jsonPath(SETTINGS + ".patch.responses['400'].content['application/json'].examples['VALIDATION_FAILED (careReminderTime)'].value.error.details.field")
                        .value("careReminderTime"))
                .andExpect(jsonPath(SETTINGS + ".patch.responses['400'].content['application/json'].examples['VALIDATION_FAILED (timeZone)'].value.error.details.field")
                        .value("timeZone"))
                .andExpect(jsonPath(SETTINGS + ".patch.responses['401'].content['application/json'].examples.TOKEN_INVALID.value.error.code")
                        .value("TOKEN_INVALID"));
    }

    @Test
    void hidesDevOnlyTestEndpointOutsideLocalAndDev() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/push-tokens/test']").doesNotExist());
    }
}
