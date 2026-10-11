package com.pabom.backend.agreement.infrastructure.persistence;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAgreementJpaRepository extends JpaRepository<UserAgreement, Long> {
}
