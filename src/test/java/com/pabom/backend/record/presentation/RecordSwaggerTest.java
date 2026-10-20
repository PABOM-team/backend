package com.pabom.backend.record.presentation;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RecordSwaggerTest {
    @Autowired MockMvc mvc;
    @Test void documentsAllOperationsSchemasSecurityAndExamples() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
          .andExpect(jsonPath("$.paths['/api/v1/records'].get.responses['200']").exists())
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['201']").exists())
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}'].get.responses['200']").exists())
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}'].patch.responses['200']").exists())
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}'].delete.responses['200']").exists())
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}/restore'].post.responses['200']").exists())
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.security[0].Authorization").isArray())
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.requestBody.content['application/json'].examples['직접 기록 추가'].value.clientRequestId").value("a1b2c3d4-e5f6-47a8-9012-123456789abc"))
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['201'].content['application/json'].examples['일정 없음'].value.success.data.routineUpdated").doesNotExist())
          .andExpect(jsonPath("$.components.schemas.CreateRecordRequest.required",hasItems("clientRequestId","petId","itemCode","occurredAt")))
          .andExpect(jsonPath("$.components.schemas.CreateRecordRequest.properties.outcome.enum",hasItems("ATE_ALL","ATE_PART","REFUSED","NOT_OBSERVED")));
          
    }

    @Test void documentsEveryDeclaredErrorCodeAtActualHttpStatus() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['409'].content['application/json'].examples.CLIENT_REQUEST_ID_REUSED.value.error.code").value("CLIENT_REQUEST_ID_REUSED"))
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['422'].content['application/json'].examples.CARE_ITEM_INVALID.value.error.code").value("CARE_ITEM_INVALID"))
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['422'].content['application/json'].examples.OUTCOME_NOT_ALLOWED.value.error.code").value("OUTCOME_NOT_ALLOWED"))
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['422'].content['application/json'].examples.VALUE_REQUIRED.value.error.code").value("VALUE_REQUIRED"))
          .andExpect(jsonPath("$.paths['/api/v1/records'].post.responses['422'].content['application/json'].examples.MEMO_REQUIRED.value.error.code").value("MEMO_REQUIRED"))
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}'].patch.responses['409'].content['application/json'].examples.RECORD_CONFLICT.value.error.code").value("RECORD_CONFLICT"))
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}'].patch.responses['422'].content['application/json'].examples.FIELD_NOT_EDITABLE.value.error.code").value("FIELD_NOT_EDITABLE"))
          .andExpect(jsonPath("$.paths['/api/v1/records/{recordId}/restore'].post.responses['410'].content['application/json'].examples.RESTORE_WINDOW_EXPIRED.value.error.code").value("RESTORE_WINDOW_EXPIRED"))
          .andExpect(jsonPath("$.paths['/api/v1/records'].get.responses['401'].content['application/json'].examples.AUTH_REQUIRED.value.error.code").value("AUTH_REQUIRED"));
    }
}
