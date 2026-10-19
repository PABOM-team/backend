package com.pabom.backend.user.application.service;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import com.pabom.backend.agreement.domain.repository.UserAgreementRepository;
import com.pabom.backend.agreement.domain.service.AgreementVersionPolicy;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.pet.domain.type.PetStatus;
import com.pabom.backend.user.application.result.UserProfileResult;
import com.pabom.backend.user.application.result.UserProfileResult.AgreementItem;
import com.pabom.backend.user.application.result.UserProfileResult.PetCount;
import com.pabom.backend.user.application.result.UserProfileResult.RequiredAgreementItem;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserAgreementRepository agreementRepository;
    private final AgreementVersionPolicy versionPolicy;
    private final PetRepository petRepository;

    @Transactional(readOnly = true)
    public UserProfileResult get(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.TOKEN_INVALID));
        if (user.isWithdrawn()) {
            throw new BusinessException(AuthErrorCode.USER_WITHDRAWN);
        }

        List<UserAgreement> histories = agreementRepository.findAllByUserId(userId);
        List<AgreementItem> agreements = histories.stream()
                .map(agreement -> new AgreementItem(
                        agreement.getType(), agreement.getVersion(), agreement.isAgreed(),
                        agreement.getAgreedAt()))
                .toList();
        List<RequiredAgreementItem> required = java.util.Arrays.stream(AgreementType.values())
                .filter(AgreementType::isRequired)
                .map(type -> new RequiredAgreementItem(type, versionPolicy.currentVersion(type), true))
                .toList();

        Map<AgreementType, UserAgreement> latest = new EnumMap<>(AgreementType.class);
        histories.forEach(agreement -> latest.putIfAbsent(agreement.getType(), agreement));
        boolean needsReconsent = required.stream().anyMatch(requirement -> {
            UserAgreement agreement = latest.get(requirement.type());
            return agreement == null || !agreement.isAgreed()
                    || !requirement.currentVersion().equals(agreement.getVersion());
        });

        return new UserProfileResult(
                user.getId(), user.getNickname(), user.getStatus(), user.getRole(), null,
                agreements, required, needsReconsent,
                new PetCount(
                        petRepository.countByUserIdAndStatus(userId, PetStatus.ACTIVE),
                        petRepository.countByUserIdAndStatus(userId, PetStatus.ENDED)
                ),
                user.getCreatedAt()
        );
    }
}
