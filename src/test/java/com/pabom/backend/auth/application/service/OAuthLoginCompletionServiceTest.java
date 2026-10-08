package com.pabom.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.application.result.OAuthLoginCompletionResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.token.ServiceTokenProvider;
import com.pabom.backend.auth.infrastructure.token.TokenType;
import com.pabom.backend.event.domain.entity.LoginEvent;
import com.pabom.backend.event.infrastructure.persistence.LoginEventJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.domain.type.UserStatus;
import com.pabom.backend.user.infrastructure.persistence.RefreshTokenJpaRepository;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OAuthLoginCompletionServiceTest {

    @Autowired
    private OAuthLoginCompletionService service;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @Autowired
    private LoginEventJpaRepository loginEventRepository;

    @Autowired
    private ServiceTokenProvider tokenProvider;

    @Test
    void createsPendingUserWithTemporaryNicknameAndIssuesOnlySignupToken() {
        OAuthLoginCompletionResult result = service.complete(
                new OAuthLoginResult(OAuthProvider.KAKAO, "provider-id", null)
        );

        assertThat(result.tokenType())
                .isEqualTo(OAuthLoginCompletionResult.LoginTokenType.SIGNUP);
        assertThat(result.expiresIn()).isEqualTo(600);
        assertThat(result.isNewUser()).isTrue();
        assertThat(result.refreshToken()).isNull();
        assertThat(result.user().getStatus()).isEqualTo(UserStatus.PENDING_TERMS);
        assertThat(result.user().getNickname()).matches("파봄 사용자\\d{4}");
        assertThat(refreshTokenRepository.count()).isZero();
        assertThat(tokenProvider.validateAndGetUserId(result.token(), TokenType.SIGNUP))
                .isEqualTo(result.user().getId());
        assertThatThrownBy(() -> tokenProvider.validateAndGetUserId(
                result.token(),
                TokenType.ACCESS
        )).isInstanceOf(IllegalArgumentException.class);

        LoginEvent event = loginEventRepository.findAll().getFirst();
        assertThat(event.getEventCode()).isEqualTo("EV-02");
        assertThat(event.getEventName()).isEqualTo("login_succeeded");
        assertThat(event.isNewUser()).isTrue();
    }

    @Test
    void keepsExistingPendingUserProfileAndIssuesSignupTokenAgain() {
        User existing = User.pending(
                OAuthProvider.GOOGLE,
                "provider-id",
                "기존 닉네임",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        userRepository.save(existing);

        OAuthLoginCompletionResult result = service.complete(
                new OAuthLoginResult(OAuthProvider.GOOGLE, "provider-id", "변경 시도")
        );

        assertThat(result.tokenType())
                .isEqualTo(OAuthLoginCompletionResult.LoginTokenType.SIGNUP);
        assertThat(result.isNewUser()).isFalse();
        assertThat(result.user().getNickname()).isEqualTo("기존 닉네임");
        assertThat(result.user().getStatus()).isEqualTo(UserStatus.PENDING_TERMS);
        assertThat(result.user().getLastLoginAt())
                .isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(refreshTokenRepository.count()).isZero();
    }

    @Test
    void issuesAccessAndHashedRefreshTokenForActiveUser() {
        User active = User.pending(
                OAuthProvider.KAKAO,
                "provider-id",
                "활성 회원",
                Instant.parse("2026-01-01T00:00:00Z")
        );
        active.activate(Instant.parse("2026-01-02T00:00:00Z"));
        userRepository.save(active);

        OAuthLoginCompletionResult result = service.complete(
                new OAuthLoginResult(OAuthProvider.KAKAO, "provider-id", "변경 시도")
        );

        assertThat(result.tokenType())
                .isEqualTo(OAuthLoginCompletionResult.LoginTokenType.ACCESS);
        assertThat(result.expiresIn()).isEqualTo(1800);
        assertThat(result.refreshExpiresIn()).isEqualTo(1209600);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(tokenProvider.validateAndGetUserId(result.token(), TokenType.ACCESS))
                .isEqualTo(active.getId());

        RefreshToken savedToken = refreshTokenRepository.findAll().getFirst();
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getTokenHash()).isNotEqualTo(result.refreshToken());
        assertThat(savedToken.getFamilyId()).isNotBlank();
        assertThat(savedToken.getUserId()).isEqualTo(active.getId());
        assertThat(loginEventRepository.findAll().getFirst().isNewUser()).isFalse();
    }
}
