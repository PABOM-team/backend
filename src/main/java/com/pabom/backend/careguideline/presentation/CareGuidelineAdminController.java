package com.pabom.backend.careguideline.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.careguideline.application.command.CareGuidelineImportFiles;
import com.pabom.backend.careguideline.application.service.CareGuidelineImportService;
import com.pabom.backend.careguideline.domain.type.CareGuidelineImportMode;
import com.pabom.backend.careguideline.presentation.docs.CareGuidelineAdminControllerDocs;
import com.pabom.backend.careguideline.presentation.response.CareGuidelineImportResponse;
import com.pabom.backend.global.error.*;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/api/v1/admin/care-standards", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CareGuidelineAdminController implements CareGuidelineAdminControllerDocs {
    private final CareGuidelineImportService service;

    @Override
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseBody<CareGuidelineImportResponse>> importCsv(
            @RequestParam String versionLabel,
            @RequestParam CareGuidelineImportMode mode,
            @RequestParam(defaultValue = "false") boolean publish,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestPart("stages") MultipartFile stages,
            @RequestPart("items") MultipartFile items,
            @RequestPart("standards") MultipartFile standards,
            @RequestPart("stageThresholds") MultipartFile stageThresholds,
            @RequestPart("descriptions") MultipartFile descriptions,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request) {
        requireAuthentication(authUser, request);
        CareGuidelineImportResponse response = service.importCsv(authUser.userId(), versionLabel, mode,
                publish, effectiveFrom, new CareGuidelineImportFiles(stages, items, standards, stageThresholds, descriptions));
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    private void requireAuthentication(AuthUser user, HttpServletRequest request) {
        if (user != null) return;
        Object code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (code instanceof BaseErrorCode errorCode) throw new BusinessException(errorCode);
        throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
    }
}
