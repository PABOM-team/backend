package com.pabom.backend.agreement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand;
import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand.Agreement;
import com.pabom.backend.agreement.application.result.AgreementCompletionResult;
import com.pabom.backend.agreement.domain.error.AgreementErrorCode;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.agreement.infrastructure.persistence.UserAgreementJpaRepository;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.token.ServiceTokenProvider;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.type.UserStatus;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AgreementCompletionServiceTest {

    @Autowired
    private AgreementCompletionService service;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private UserAgreementJpaRepository agreementRepository;

    @Autowired
    private ServiceTokenProvider tokenProvider;

    private User pendingUser;
    private String signupToken;

    @BeforeEach
    void setUp() {
        agreementRepository.deleteAll();
        userRepository.deleteAll();
        pendingUser = userRepository.save(User.pending(
                OAuthProvider.GOOGLE,
                "agreement-test-user",
                "가입 대기 사용자",
                Instant.parse("2026-10-09T00:00:00Z")
        ));
        signupToken = tokenProvider.issueSignupToken(
                pendingUser.getId(),
                Instant.now()
        ).value();
    }

    @Test
    void completesSignupWithRequiredAgreementsAndIssuesAccessToken() {
        AgreementCompletionResult result = service.complete(signupToken, validCommand(true));

        assertThat(result.userId()).isEqualTo(pendingUser.getId());
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.expiresIn()).isEqualTo(1800);
        assertThat(tokenProvider.validateAccessToken(result.accessToken()))
                .isEqualTo(pendingUser.getId());
        assertThat(userRepository.findById(pendingUser.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
        assertThat(agreementRepository.findAll()).hasSize(4);
    }

    @Test
    void completesSignupWhenMarketingIsNotAgreed() {
        AgreementCompletionResult result = service.complete(signupToken, validCommand(false));

        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(agreementRepository.findAll())
                .filteredOn(agreement -> agreement.getType() == AgreementType.MARKETING)
                .singleElement()
                .satisfies(agreement -> assertThat(agreement.isAgreed()).isFalse());
    }

    @Test
    void rejectsTermsNotAgreed() {
        CompleteAgreementsCommand command = replace(validCommand(false), AgreementType.TERMS, false);

        assertError(command, AgreementErrorCode.REQUIRED_AGREEMENT_MISSING);
    }

    @Test
    void rejectsMissingPrivacyAgreement() {
        CompleteAgreementsCommand command = new CompleteAgreementsCommand(
                validCommand(false).agreements().stream()
                        .filter(agreement -> agreement.type() != AgreementType.PRIVACY)
                        .toList()
        );

        assertError(command, AgreementErrorCode.REQUIRED_AGREEMENT_MISSING);
    }

    @Test
    void rejectsAgeAgreementNotAgreed() {
        CompleteAgreementsCommand command = replace(validCommand(false), AgreementType.AGE_14, false);

        assertError(command, AgreementErrorCode.REQUIRED_AGREEMENT_MISSING);
    }

    @Test
    void rejectsAgreementVersionMismatch() {
        List<Agreement> agreements = new ArrayList<>(validCommand(false).agreements());
        agreements.set(0, new Agreement(AgreementType.TERMS, "0.9", true));

        assertError(
                new CompleteAgreementsCommand(agreements),
                AgreementErrorCode.AGREEMENT_VERSION_MISMATCH
        );
    }

    @Test
    void rejectsDuplicateAgreementType() {
        List<Agreement> agreements = new ArrayList<>(validCommand(false).agreements());
        agreements.add(new Agreement(AgreementType.TERMS, "1.0", true));

        assertError(new CompleteAgreementsCommand(agreements), AuthErrorCode.VALIDATION_FAILED);
    }

    @Test
    void rejectsAccessTokenAtSignupEndpoint() {
        String accessToken = tokenProvider.issueAccessToken(
                pendingUser.getId(),
                Instant.now()
        ).value();

        assertThatThrownBy(() -> service.complete(accessToken, validCommand(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    void rejectsExpiredSignupToken() {
        String expiredToken = tokenProvider.issueSignupToken(
                pendingUser.getId(),
                Instant.parse("2020-01-01T00:00:00Z")
        ).value();

        assertThatThrownBy(() -> service.complete(expiredToken, validCommand(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
    }

    @Test
    void rejectsReusedSignupTokenAfterCompletion() {
        service.complete(signupToken, validCommand(false));

        assertThatThrownBy(() -> service.complete(signupToken, validCommand(false)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(AuthErrorCode.INVALID_TOKEN));
        assertThat(agreementRepository.findAll()).hasSize(4);
    }

    private CompleteAgreementsCommand validCommand(boolean marketingAgreed) {
        return new CompleteAgreementsCommand(List.of(
                new Agreement(AgreementType.TERMS, "1.0", true),
                new Agreement(AgreementType.PRIVACY, "1.0", true),
                new Agreement(AgreementType.AGE_14, "1.0", true),
                new Agreement(AgreementType.MARKETING, "1.0", marketingAgreed)
        ));
    }

    private CompleteAgreementsCommand replace(
            CompleteAgreementsCommand command,
            AgreementType type,
            boolean agreed
    ) {
        return new CompleteAgreementsCommand(command.agreements().stream()
                .map(item -> item.type() == type
                        ? new Agreement(type, item.version(), agreed)
                        : item)
                .toList());
    }

    private void assertError(
            CompleteAgreementsCommand command,
            Object expectedErrorCode
    ) {
        assertThatThrownBy(() -> service.complete(signupToken, command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(expectedErrorCode));
        assertThat(agreementRepository.count()).isZero();
        assertThat(userRepository.findById(pendingUser.getId()).orElseThrow().getStatus())
                .isEqualTo(UserStatus.PENDING_TERMS);
    }
}
