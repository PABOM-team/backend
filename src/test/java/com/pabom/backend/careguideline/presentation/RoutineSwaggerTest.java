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
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/complete'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/skip'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/postpone'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/postpone'].post.requestBody.content['application/json'].examples['정상 미루기'].value.postponeTo").value("2026-11-25"))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/postpone'].post.requestBody.content['application/json'].examples['미루기 취소'].value.postponeTo").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/postpone'].post.responses['200'].content['application/json'].examples['정상 미루기'].value.success.data.routine.nextDueOn").value("2026-11-25"))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/postpone'].post.responses['422'].content['application/json'].examples.POSTPONE_DATE_INVALID.value.error.code").value("POSTPONE_DATE_INVALID"))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/postpone'].post.responses['422'].content['application/json'].examples.POSTPONE_DATE_INVALID.value.error.message").value("미룰 날짜는 내일부터 29일 이내이며, 현재 예정일보다 뒤여야 합니다."))
                .andExpect(jsonPath("$.components.schemas.PostponeRoutineRequest.properties.postponeTo.format").value("date"))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/complete'].post.security[0].Authorization").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/complete'].post.requestBody.content['application/json'].examples['크레스티드 게코 DIET 급여 완료'].value.outcome").value("EATING_TRACE_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/complete'].post.responses['200'].content['application/json'].examples['크레스티드 게코 DIET 급여 완료'].value.success.data.record.outcome").value("EATING_TRACE_FOUND"))
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/complete'].post.responses['422'].content['application/json'].examples.OUTCOME_NOT_ALLOWED.value.error.code").value("OUTCOME_NOT_ALLOWED"))
                .andExpect(jsonPath("$.paths['/api/v1/today'].get.security[0].Authorization").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}'].patch.requestBody.content['application/json'].schema['$ref']").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateRoutineRequest.properties.reason.enum",
                        hasItems("BOTHERSOME", "MISMATCH", "SPECIAL_CONDITION", "PREFERENCE")))
                .andExpect(jsonPath("$.components.schemas.UpdateRoutineRequest.properties.foodName.maxLength").value(20))
                .andExpect(jsonPath("$.components.schemas.TodayTask.properties.originalDueDate.format").value("date"))
                .andExpect(jsonPath("$.components.schemas.TodayTask.properties.foodName").exists())
                .andExpect(jsonPath("$.components.schemas.RoutinePostponeState.properties.id").exists())
                .andExpect(jsonPath("$.components.schemas.RoutinePostponeState.properties.nextDueOn").exists())
                .andExpect(jsonPath("$.components.schemas.RoutinePostponeState.properties.baseDueOn").exists())
                .andExpect(jsonPath("$.components.schemas.RoutinePostponeState.properties.postponedTo").exists())
                .andExpect(jsonPath("$.components.schemas.RoutinePostponeState.properties.postponedAt").exists())
                .andExpect(jsonPath("$.paths['/api/v1/routines/{routineId}/reset'].post.responses['404'].content['application/json'].examples.CARE_STANDARD_NOT_FOUND.value.error.code")
                        .value("CARE_STANDARD_NOT_FOUND"));
    }
}
