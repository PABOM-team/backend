package com.pabom.backend.file.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class FileSwaggerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void documentsMultipartSuccessSchemasAndActualErrorCodes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/files'].post.requestBody.required").value(true))
                .andExpect(jsonPath("$.paths['/api/v1/files'].post.requestBody.content['multipart/form-data']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/files'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/files'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/files'].post.responses['413']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/files/{fileId}'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/files/{fileId}'].get.responses['404']").exists())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("PET_PHOTO"),
                        org.hamcrest.Matchers.containsString("RECORD_PHOTO"),
                        org.hamcrest.Matchers.containsString("DEATH_EVIDENCE"),
                        org.hamcrest.Matchers.containsString("REPORT_RECEIPT"),
                        org.hamcrest.Matchers.containsString("INVALID_FILE_FORMAT"),
                        org.hamcrest.Matchers.containsString("UNSUPPORTED_FILE_PURPOSE"),
                        org.hamcrest.Matchers.containsString("FILE_TOO_LARGE"),
                        org.hamcrest.Matchers.containsString("FILE_NOT_FOUND")
                )));
    }
}
