package com.pabom.backend.careguideline.presentation;

import static org.hamcrest.Matchers.hasItems;
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
class RoutineSwaggerTest {
    @Autowired MockMvc mvc;

    @Test
    void documentsTodayUpdateResetSchemasSecurityAndExamples() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/today'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}'].patch.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/reset'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/today'].get.security[0].Authorization").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}'].patch.requestBody.content['application/json'].schema['$ref']").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateRoutineRequest.properties.reason.enum",
                        hasItems("BOTHERSOME", "MISMATCH", "SPECIAL_CONDITION", "PREFERENCE")))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/reset'].post.responses['404'].content['application/json'].examples.CARE_STANDARD_NOT_FOUND.value.error.code")
                        .value("CARE_STANDARD_NOT_FOUND"));
    }
}
