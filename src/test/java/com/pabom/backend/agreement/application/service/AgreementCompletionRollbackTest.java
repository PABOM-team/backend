package com.pabom.backend.agreement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand;
import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand.Agreement;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.agreement.infrastructure.persistence.UserAgreementJpaRepository;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.token.ServiceTokenProvider;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.UserStatus;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@ActiveProfiles("test")
class AgreementCompletionRollbackTest {

    @Autowired
    private AgreementCompletionService service;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private UserAgreementJpaRepository agreementJpaRepository;

    @Autowired
    private ServiceTokenProvider tokenProvider;

    @MockitoSpyBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        agreementJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
    }

    @Test
    void rollsBackAgreementHistoryAndUserStatusWhenUserSaveFails() {
        User pendingUser = userJpaRepository.save(User.pending(
                OAuthProvider.KAKAO,
                "rollback-test-user",
                "롤백 테스트 사용자",
                Instant.parse("2026-10-09T00:00:00Z")
        ));
        String signupToken = tokenProvider.issueSignupToken(
                pendingUser.getId(),
                Instant.now()
        ).value();
        doThrow(new DataAccessResourceFailureException("simulated database failure"))
                .when(userRepository)
                .save(argThat(user -> user != null && user.getStatus() == UserStatus.ACTIVE));

        assertThatThrownBy(() -> service.complete(signupToken, validCommand()))
                .isInstanceOf(DataAccessResourceFailureException.class);

        assertThat(agreementJpaRepository.count()).isZero();
        assertThat(userJpaRepository.findById(pendingUser.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.PENDING_TERMS);
    }

    private CompleteAgreementsCommand validCommand() {
        return new CompleteAgreementsCommand(List.of(
                new Agreement(AgreementType.TERMS, "1.0", true),
                new Agreement(AgreementType.PRIVACY, "1.0", true),
                new Agreement(AgreementType.AGE_14, "1.0", true),
                new Agreement(AgreementType.MARKETING, "1.0", false)
        ));
    }
}
