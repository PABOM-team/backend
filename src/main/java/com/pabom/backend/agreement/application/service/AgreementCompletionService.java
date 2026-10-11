package com.pabom.backend.agreement.application.service;

import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand;
import com.pabom.backend.agreement.application.command.CompleteAgreementsCommand.Agreement;
import com.pabom.backend.agreement.application.result.AgreementCompletionResult;
import com.pabom.backend.agreement.domain.entity.UserAgreement;
import com.pabom.backend.agreement.domain.error.AgreementErrorCode;
import com.pabom.backend.agreement.domain.repository.UserAgreementRepository;
import com.pabom.backend.agreement.domain.service.AgreementVersionPolicy;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.auth.application.port.ServiceTokenPort;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedToken;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.UserStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgreementCompletionService {

    private final ServiceTokenPort tokenProvider;
    private final UserRepository userRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final AgreementVersionPolicy versionPolicy;
    private final Clock clock;

    public AgreementCompletionService(
            ServiceTokenPort tokenProvider,
            UserRepository userRepository,
            UserAgreementRepository userAgreementRepository,
            AgreementVersionPolicy versionPolicy,
            Clock clock
    ) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.userAgreementRepository = userAgreementRepository;
        this.versionPolicy = versionPolicy;
        this.clock = clock;
    }

    @Transactional
    public AgreementCompletionResult complete(
            String signupToken,
            CompleteAgreementsCommand command
    ) {
        Long userId = tokenProvider.validateSignupToken(signupToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_TOKEN));
        if (user.getStatus() != UserStatus.PENDING_TERMS) {
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
        }

        List<Agreement> agreements = command.agreements();
        validateNoDuplicates(agreements);
        validateRequiredAgreements(agreements);
        validateVersions(agreements);

        Instant completedAt = clock.instant();
        List<UserAgreement> histories = agreements.stream()
                .map(agreement -> new UserAgreement(
                        userId,
                        agreement.type(),
                        agreement.version(),
                        agreement.agreed(),
                        completedAt
                ))
                .toList();
        userAgreementRepository.saveAll(histories);
        user.activate(completedAt);
        userRepository.save(user);

        IssuedToken accessToken = tokenProvider.issueAccessToken(userId, completedAt);
        return new AgreementCompletionResult(
                userId,
                user.getStatus(),
                accessToken.value(),
                accessToken.expiresIn()
        );
    }

    private void validateNoDuplicates(List<Agreement> agreements) {
        Set<AgreementType> types = EnumSet.noneOf(AgreementType.class);
        boolean duplicated = agreements.stream()
                .map(Agreement::type)
                .anyMatch(type -> !types.add(type));
        if (duplicated) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }
    }

    private void validateRequiredAgreements(List<Agreement> agreements) {
        boolean allRequiredAgreed = EnumSet.allOf(AgreementType.class).stream()
                .filter(AgreementType::isRequired)
                .allMatch(requiredType -> agreements.stream().anyMatch(agreement ->
                        agreement.type() == requiredType && agreement.agreed()
                ));
        if (!allRequiredAgreed) {
            throw new BusinessException(AgreementErrorCode.REQUIRED_AGREEMENT_MISSING);
        }
    }

    private void validateVersions(List<Agreement> agreements) {
        boolean mismatched = agreements.stream().anyMatch(agreement ->
                !versionPolicy.currentVersion(agreement.type()).equals(agreement.version())
        );
        if (mismatched) {
            throw new BusinessException(AgreementErrorCode.AGREEMENT_VERSION_MISMATCH);
        }
    }
}
