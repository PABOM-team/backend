package com.pabom.backend.agreement.domain.repository;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import java.util.List;

public interface UserAgreementRepository {

    void saveAll(List<UserAgreement> agreements);

    List<UserAgreement> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
