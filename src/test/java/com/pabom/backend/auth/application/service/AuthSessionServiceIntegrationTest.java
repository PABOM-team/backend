package com.pabom.backend.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedRefreshToken;
import com.pabom.backend.auth.application.result.TokenRefreshResult;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.token.ServiceTokenProvider;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.infrastructure.persistence.RefreshTokenJpaRepository;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthSessionServiceIntegrationTest {

    @Autowired
    private AuthSessionService authSessionService;

    @Autowired
    private ServiceTokenProvider tokenProvider;

    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private Clock clock;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void rotatesRefreshTokenAndKeepsTokenFamily() {
        TokenFixture fixture = activeTokenFixture();

        TokenRefreshResult result = authSessionService.refresh(fixture.rawToken());

        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.expiresIn()).isEqualTo(1800);
        assertThat(result.refreshToken()).isNotEqualTo(fixture.rawToken());
        assertThat(result.refreshExpiresIn()).isEqualTo(1209600);

        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).hasSize(2);
        assertThat(tokens).extracting(RefreshToken::getFamilyId)
                .containsOnly(fixture.familyId());
        assertThat(tokens).filteredOn(RefreshToken::isRotated).hasSize(1);
        assertThat(tokens).filteredOn(token -> token.isActiveAt(clock.instant())).hasSize(1);
    }

    @Test
    void rejectsMissingAndUnknownRefreshTokens() {
        assertRefreshInvalid(null);
        assertRefreshInvalid("unknown-token");
    }

    @Test
    void revokesWholeFamilyWhenRotatedTokenIsReused() {
        TokenFixture fixture = activeTokenFixture();
        authSessionService.refresh(fixture.rawToken());

        assertThatThrownBy(() -> authSessionService.refresh(fixture.rawToken()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.REFRESH_REUSED));

        assertThat(refreshTokenRepository.findAll())
                .noneMatch(token -> token.isActiveAt(clock.instant()));
    }

    @Test
    void serializesConcurrentRotationAndRevokesFamilyOnReuse() throws Exception {
        TokenFixture fixture = activeTokenFixture();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Object> first = executor.submit(() -> refreshAfterSignal(fixture.rawToken(), ready, start));
        Future<Object> second = executor.submit(() -> refreshAfterSignal(fixture.rawToken(), ready, start));
        ready.await();
        start.countDown();

        List<Object> outcomes = List.of(first.get(), second.get());
        assertThat(outcomes).filteredOn(TokenRefreshResult.class::isInstance).hasSize(1);
        assertThat(outcomes).filteredOn(outcome ->
                outcome instanceof BusinessException exception
                        && exception.errorCode() == AuthErrorCode.REFRESH_REUSED
        ).hasSize(1);
        assertThat(refreshTokenRepository.findAll())
                .noneMatch(token -> token.isActiveAt(clock.instant()));
    }

    @Test
    void rejectsWithdrawnUser() {
        TokenFixture fixture = activeTokenFixture();
        User user = userRepository.findById(fixture.userId()).orElseThrow();
        user.withdraw(clock.instant());
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> authSessionService.refresh(fixture.rawToken()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.USER_WITHDRAWN));
    }

    @Test
    void logoutIsIdempotentAndRevokesRotatedFamily() {
        TokenFixture fixture = activeTokenFixture();
        authSessionService.refresh(fixture.rawToken());

        authSessionService.logout(fixture.rawToken());
        authSessionService.logout(fixture.rawToken());
        authSessionService.logout(null);
        authSessionService.logout("unknown-token");

        assertThat(refreshTokenRepository.findAll())
                .noneMatch(token -> token.isActiveAt(clock.instant()));
    }

    private Object refreshAfterSignal(
            String rawToken,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            return authSessionService.refresh(rawToken);
        } catch (BusinessException exception) {
            return exception;
        }
    }

    private TokenFixture activeTokenFixture() {
        Instant now = clock.instant();
        User user = User.pending(OAuthProvider.GOOGLE, "provider-" + now.toEpochMilli(), "사용자", now);
        user.activate(now);
        user = userRepository.saveAndFlush(user);

        IssuedRefreshToken issued = tokenProvider.issueRefreshToken(user.getId(), now);
        refreshTokenRepository.saveAndFlush(new RefreshToken(
                issued.hash(),
                user.getId(),
                issued.familyId(),
                issued.issuedAt(),
                issued.expiresAt()
        ));
        return new TokenFixture(user.getId(), issued.value(), issued.familyId());
    }

    private void assertRefreshInvalid(String rawToken) {
        assertThatThrownBy(() -> authSessionService.refresh(rawToken))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.REFRESH_INVALID));
    }

    private record TokenFixture(Long userId, String rawToken, String familyId) {
    }
}
