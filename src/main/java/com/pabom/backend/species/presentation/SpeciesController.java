package com.pabom.backend.species.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.species.application.service.SpeciesQueryService;
import com.pabom.backend.species.presentation.response.MorphResponse;
import com.pabom.backend.species.presentation.response.SpeciesResponse;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/species")
@RequiredArgsConstructor
@Tag(name = "Species", description = "파충류 종 및 모프 조회 API")
public class SpeciesController {

    private final SpeciesQueryService service;

    @GetMapping
    @Operation(summary = "파충류 종 목록 조회", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    public ResponseEntity<ApiResponseBody<List<SpeciesResponse>>> getSpecies(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        List<SpeciesResponse> response = service.getSpecies().stream()
                .map(SpeciesResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    @GetMapping("/{speciesId}/morphs")
    @Operation(summary = "종별 모프 목록 조회", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    public ResponseEntity<ApiResponseBody<List<MorphResponse>>> getMorphs(
            @PathVariable Long speciesId,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        List<MorphResponse> response = service.getMorphs(speciesId).stream()
                .map(MorphResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    private void requireAuthentication(AuthUser authUser) {
        if (authUser == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
