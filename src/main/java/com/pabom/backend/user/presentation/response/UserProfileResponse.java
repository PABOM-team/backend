package com.pabom.backend.user.presentation.response;

import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.user.application.result.UserProfileResult;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "현재 로그인한 사용자 정보")
public record UserProfileResponse(
        @Schema(description = "사용자 ID", example = "101") Long id,
        @Schema(description = "닉네임", example = "테스트", nullable = true) String nickname,
        @Schema(description = "회원 상태", example = "ACTIVE") UserStatus status,
        @Schema(description = "회원 권한", example = "USER") UserRole role,
        @Schema(description = "지역 정보. 현재 미지원", nullable = true) Object region,
        List<AgreementResponse> agreements,
        List<RequiredAgreementResponse> requiredAgreements,
        @Schema(description = "필수 약관 재동의 필요 여부", example = "false")
        boolean needsReconsent,
        PetCountResponse petCount,
        @Schema(description = "가입 시각", example = "2026-10-17T02:00:00Z") Instant createdAt
) {

    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.id(),
                result.nickname(),
                result.status(),
                result.role(),
                result.region(),
                result.agreements().stream().map(AgreementResponse::from).toList(),
                result.requiredAgreements().stream().map(RequiredAgreementResponse::from).toList(),
                result.needsReconsent(),
                new PetCountResponse(result.petCount().active(), result.petCount().ended()),
                result.createdAt()
        );
    }

    public record AgreementResponse(
            @Schema(description = "약관 종류", example = "TERMS") AgreementType type,
            @Schema(description = "동의한 약관 버전", example = "1.0") String version,
            @Schema(description = "동의 여부", example = "true") boolean agreed,
            @Schema(description = "동의 시각", example = "2026-10-17T03:00:00Z") Instant agreedAt
    ) {
        private static AgreementResponse from(UserProfileResult.AgreementItem item) {
            return new AgreementResponse(item.type(), item.version(), item.agreed(), item.agreedAt());
        }
    }

    public record RequiredAgreementResponse(
            @Schema(description = "약관 종류", example = "TERMS") AgreementType type,
            @Schema(description = "현재 약관 버전", example = "1.0") String currentVersion,
            @Schema(description = "필수 여부", example = "true") boolean required
    ) {
        private static RequiredAgreementResponse from(
                UserProfileResult.RequiredAgreementItem item
        ) {
            return new RequiredAgreementResponse(
                    item.type(), item.currentVersion(), item.required());
        }
    }

    public record PetCountResponse(
            @Schema(description = "활성 개체 수", example = "0") long active,
            @Schema(description = "종료 개체 수", example = "0") long ended
    ) {
    }
}
