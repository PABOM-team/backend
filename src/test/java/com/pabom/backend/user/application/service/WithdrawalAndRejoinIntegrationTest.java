package com.pabom.backend.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.agreement.infrastructure.persistence.UserAgreementJpaRepository;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedRefreshToken;
import com.pabom.backend.auth.application.result.OAuthLoginCompletionResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.application.service.OAuthLoginCompletionService;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.token.ServiceTokenProvider;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.domain.type.CleanupJobStatus;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import com.pabom.backend.user.infrastructure.persistence.RefreshTokenJpaRepository;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import com.pabom.backend.user.infrastructure.persistence.WithdrawalCleanupJobJpaRepository;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WithdrawalAndRejoinIntegrationTest {

    @Autowired WithdrawalService withdrawalService;
    @Autowired OAuthLoginCompletionService loginService;
    @Autowired UserJpaRepository users;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired UserAgreementJpaRepository agreements;
    @Autowired WithdrawalCleanupJobJpaRepository cleanupJobs;
    @Autowired ServiceTokenProvider tokens;
    @Autowired Clock clock;

    @ParameterizedTest
    @EnumSource(OAuthProvider.class)
    void withdrawsPabomAccountAndRejoinsAsEntirelyNewUser(OAuthProvider provider) {
        Instant now = clock.instant();
        String providerId = provider.name().toLowerCase() + "-rejoin";
        User original = User.pending(provider, providerId, "기존 닉네임", now);
        original.activate(now);
        original = users.saveAndFlush(original);
        agreements.save(new UserAgreement(
                original.getId(), AgreementType.TERMS, "1.0", true, now));
        IssuedRefreshToken issued = tokens.issueRefreshToken(original.getId(), now);
        refreshTokens.saveAndFlush(new RefreshToken(
                issued.hash(), original.getId(), issued.familyId(),
                issued.issuedAt(), issued.expiresAt()));

        withdrawalService.withdraw(original.getId());

        User withdrawn = users.findById(original.getId()).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(withdrawn.getProvider()).isEqualTo(provider);
        assertThat(withdrawn.getProviderId()).isNull();
        assertThat(withdrawn.getNickname()).isNull();
        assertThat(withdrawn.getLastLoginAt()).isNull();
        assertThat(withdrawn.getWithdrawnAt()).isNotNull();
        assertThat(refreshTokens.findAll())
                .allMatch(token -> token.getRevokedAt() != null);
        assertThat(cleanupJobs.findAll()).singleElement()
                .extracting(job -> job.getStatus()).isEqualTo(CleanupJobStatus.PENDING);

        OAuthLoginCompletionResult rejoined = loginService.complete(
                new OAuthLoginResult(provider, providerId, "신규 닉네임"));

        assertThat(rejoined.isNewUser()).isTrue();
        assertThat(rejoined.user().getId()).isNotEqualTo(original.getId());
        assertThat(rejoined.user().getStatus()).isEqualTo(UserStatus.PENDING_TERMS);
        assertThat(rejoined.user().getRole()).isEqualTo(UserRole.USER);
        assertThat(agreements.findAllByUserIdOrderByAgreedAtDesc(rejoined.user().getId())).isEmpty();
    }
}
