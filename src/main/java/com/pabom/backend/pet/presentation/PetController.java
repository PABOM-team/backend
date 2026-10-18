package com.pabom.backend.pet.presentation;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.config.SwaggerConfig;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.response.ApiResponseBody;
import com.pabom.backend.global.security.AuthUser;
import com.pabom.backend.pet.application.service.PetCommandService;
import com.pabom.backend.pet.application.service.PetQueryService;
import com.pabom.backend.pet.presentation.request.CreatePetRequest;
import com.pabom.backend.pet.presentation.request.UpdatePetRequest;
import com.pabom.backend.pet.presentation.response.PetResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
@Tag(name = "Pet", description = "파충류 개체 등록 및 조회 API")
public class PetController {

    private final PetCommandService commandService;
    private final PetQueryService queryService;

    @PostMapping
    @Operation(summary = "개체 등록", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    public ResponseEntity<ApiResponseBody<PetResponse>> create(
            @Valid @RequestBody CreatePetRequest body,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        PetResponse response = PetResponse.from(commandService.create(authUser.userId(), body.toCommand()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseBody.success(response, request));
    }

    @GetMapping
    @Operation(summary = "내 개체 목록 조회", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    public ResponseEntity<ApiResponseBody<List<PetResponse>>> getAll(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        List<PetResponse> response = queryService.getAll(authUser.userId()).stream()
                .map(PetResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponseBody.success(response, request));
    }

    @GetMapping("/{petId}")
    @Operation(summary = "내 개체 상세 조회", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    public ResponseEntity<ApiResponseBody<PetResponse>> get(
            @PathVariable Long petId,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        return ResponseEntity.ok(ApiResponseBody.success(
                PetResponse.from(queryService.get(authUser.userId(), petId)),
                request
        ));
    }

    @PatchMapping("/{petId}")
    @Operation(summary = "내 개체 기본 정보 수정", security = @SecurityRequirement(name = SwaggerConfig.AUTHORIZATION))
    public ResponseEntity<ApiResponseBody<PetResponse>> update(
            @PathVariable Long petId,
            @Valid @RequestBody UpdatePetRequest body,
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletRequest request
    ) {
        requireAuthentication(authUser);
        return ResponseEntity.ok(ApiResponseBody.success(
                PetResponse.from(commandService.update(authUser.userId(), petId, body.toCommand())),
                request
        ));
    }

    private void requireAuthentication(AuthUser authUser) {
        if (authUser == null) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
