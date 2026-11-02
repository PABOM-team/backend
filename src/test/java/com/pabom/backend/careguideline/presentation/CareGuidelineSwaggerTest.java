package com.pabom.backend.careguideline.presentation;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CareGuidelineSwaggerTest {
    @Autowired MockMvc mvc;

    @Test
    void documentsApi92MultipartContractAndAllResponses() throws Exception {
        String operation = "$.paths['/api/v1/admin/care-standards/import'].post";
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(operation + ".requestBody.content['multipart/form-data']").exists())
                .andExpect(jsonPath(operation + ".requestBody.content['multipart/form-data'].schema.required",
                        hasItems("stages", "items", "standards", "stageThresholds", "descriptions")))
                .andExpect(jsonPath(operation + ".requestBody.content['multipart/form-data'].schema.properties.stages.format")
                        .value("binary"))
                .andExpect(jsonPath(operation + ".parameters[*].name",
                        hasItems("versionLabel", "mode", "publish", "effectiveFrom")))
                .andExpect(jsonPath(operation + ".responses", allOf(
                        hasKey("200"), hasKey("400"), hasKey("401"), hasKey("403"), hasKey("409"))))
                .andExpect(jsonPath(operation + ".security[0].Authorization").isArray());
    }
}
