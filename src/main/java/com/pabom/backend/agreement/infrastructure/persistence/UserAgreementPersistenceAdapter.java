package com.pabom.backend.agreement.infrastructure.persistence;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import com.pabom.backend.agreement.domain.repository.UserAgreementRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserAgreementPersistenceAdapter implements UserAgreementRepository {

    private final UserAgreementJpaRepository repository;

    @Override
    public void saveAll(List<UserAgreement> agreements) {
        repository.saveAll(agreements);
    }
}
