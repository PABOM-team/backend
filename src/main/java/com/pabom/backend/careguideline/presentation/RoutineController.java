package com.pabom.backend.careguideline.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.careguideline.application.result.RoutineMutationResult;
import com.pabom.backend.careguideline.application.result.TodayResult;
import com.pabom.backend.careguideline.application.service.RoutineCommandService;
import com.pabom.backend.careguideline.application.service.TodayQueryService;
import com.pabom.backend.careguideline.presentation.docs.RoutineControllerDocs;
import com.pabom.backend.careguideline.presentation.request.UpdateRoutineRequest;
import com.pabom.backend.global.error.BaseErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.global.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RoutineController implements RoutineControllerDocs {
    private final TodayQueryService todayQueryService;
    private final RoutineCommandService routineCommandService;

    @Override
    @GetMapping("/today")
    public ResponseEntity<ApiResponseBody<TodayResult>> today(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) LocalDate date,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(
                todayQueryService.get(authUser.userId(), petId, date), request));
    }

    @Override
    @PatchMapping(value = "/routines/{routineId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponseBody<RoutineMutationResult>> update(
            @PathVariable Long routineId,
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UpdateRoutineRequest body,
            HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(
                routineCommandService.update(authUser.userId(), routineId, body), request));
    }

    @Override
    @PostMapping("/routines/{routineId}/reset")
    public ResponseEntity<ApiResponseBody<RoutineMutationResult>> reset(
            @PathVariable Long routineId,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request) {
        requireAuthentication(authUser, request);
        return ResponseEntity.ok(ApiResponseBody.success(
                routineCommandService.reset(authUser.userId(), routineId), request));
    }

    private void requireAuthentication(AuthUser user, HttpServletRequest request) {
        if (user != null) return;
        Object code = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        if (code instanceof BaseErrorCode errorCode) throw new BusinessException(errorCode);
        throw new BusinessException(AuthErrorCode.AUTH_REQUIRED);
    }
}
