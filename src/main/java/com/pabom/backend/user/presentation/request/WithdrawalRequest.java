package com.pabom.backend.user.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 확인 요청")
public record WithdrawalRequest(
        @Schema(description = "회원 탈퇴 확인. 반드시 true여야 합니다.", example = "true")
        Boolean confirm
) {
}
