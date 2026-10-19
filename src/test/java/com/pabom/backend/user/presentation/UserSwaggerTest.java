package com.pabom.backend.user.presentation;

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
class UserSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsUserTagConcreteResponsesAndAllContractErrors() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/users/me']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/me']").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.tags[0]").value("User"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.security[0].Authorization")
                        .isArray())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/UserProfileApiResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401'].content['application/json'].examples.AUTH_REQUIRED.value.error.code")
                        .value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401'].content['application/json'].examples.TOKEN_EXPIRED.value.error.code")
                        .value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401'].content['application/json'].examples.TOKEN_INVALID.value.error.code")
                        .value("TOKEN_INVALID"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].get.responses['401'].content['application/json'].examples.USER_WITHDRAWN.value.error.code")
                        .value("USER_WITHDRAWN"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].delete.requestBody.required")
                        .value(true))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].delete.responses['204']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].delete.responses['422'].content['application/json'].examples.CONFIRM_REQUIRED.value.error.code")
                        .value("CONFIRM_REQUIRED"));
    }
}
