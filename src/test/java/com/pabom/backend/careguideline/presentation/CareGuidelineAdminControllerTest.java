package com.pabom.backend.careguideline.presentation;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.pabom.backend.careguideline.application.command.CareGuidelineImportFiles;
import com.pabom.backend.careguideline.application.service.CareGuidelineImportService;
import com.pabom.backend.careguideline.domain.error.CareGuidelineErrorCode;
import com.pabom.backend.careguideline.domain.type.*;
import com.pabom.backend.careguideline.presentation.response.CareGuidelineImportResponse;
import com.pabom.backend.global.error.*;
import com.pabom.backend.global.security.AuthUser;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CareGuidelineAdminControllerTest {
    private CareGuidelineImportService service;
    private MockMvc mvc;
    private MockMultipartFile stages;
    private MockMultipartFile items;
    private MockMultipartFile standards;
    private MockMultipartFile thresholds;
    private MockMultipartFile descriptions;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(CareGuidelineImportService.class);
        mvc = MockMvcBuilders.standaloneSetup(new CareGuidelineAdminController(service))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(7L), null, List.of()));
        stages = csv("stages"); items = csv("items"); standards = csv("standards");
        thresholds = csv("stageThresholds"); descriptions = csv("descriptions");
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void importsFiveCsvFilesWithDocumentedRequestContract() throws Exception {
        given(service.importCsv(eq(7L), eq("1.2"), eq(CareGuidelineImportMode.DRY_RUN),
                eq(false), isNull(), any(CareGuidelineImportFiles.class)))
                .willReturn(new CareGuidelineImportResponse("1.2", CareGuidelineImportMode.DRY_RUN,
                        true, false, GuidelineVersionStatus.DRAFT,
                        new CareGuidelineImportResponse.ImportCounts(3, 10, 46, 5, 14)));

        mvc.perform(multipart("/api/v1/admin/care-standards/import")
                        .file(stages).file(items).file(standards).file(thresholds).file(descriptions)
                        .param("versionLabel", "1.2").param("mode", "DRY_RUN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.success.data.persisted").value(false))
                .andExpect(jsonPath("$.success.data.counts.standards").value(46));
    }

    @Test
    void requiresAuthenticationAndAllFiles() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(multipart("/api/v1/admin/care-standards/import")
                        .file(stages).file(items).file(standards).file(thresholds).file(descriptions)
                        .param("versionLabel", "1.2").param("mode", "DRY_RUN"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(7L), null, List.of()));
        mvc.perform(multipart("/api/v1/admin/care-standards/import")
                        .file(stages).file(items).file(standards).file(thresholds)
                        .param("versionLabel", "1.2").param("mode", "DRY_RUN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details.field").value("descriptions"));
    }

    @Test
    void returnsForbiddenForNonAdmin() throws Exception {
        given(service.importCsv(anyLong(), anyString(), any(), anyBoolean(), any(), any()))
                .willThrow(new BusinessException(CareGuidelineErrorCode.ADMIN_REQUIRED));

        mvc.perform(multipart("/api/v1/admin/care-standards/import")
                        .file(stages).file(items).file(standards).file(thresholds).file(descriptions)
                        .param("versionLabel", "1.2").param("mode", "COMMIT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ADMIN_REQUIRED"));
    }

    private MockMultipartFile csv(String part) {
        return new MockMultipartFile(part, part + ".csv", "text/csv", "header\nvalue".getBytes());
    }
}
