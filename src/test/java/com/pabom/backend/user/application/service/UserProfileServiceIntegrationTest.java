package com.pabom.backend.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.agreement.infrastructure.persistence.UserAgreementJpaRepository;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.user.application.result.UserProfileResult;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserProfileServiceIntegrationTest {

    @Autowired UserProfileService service;
    @Autowired UserJpaRepository users;
    @Autowired UserAgreementJpaRepository agreements;
    @Autowired PetJpaRepository pets;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void returnsPendingUserCurrentAgreementsRoleAndCountsExcludingDeletedPets() {
        Instant now = Instant.parse("2026-10-19T00:00:00Z");
        User user = users.saveAndFlush(User.pending(OAuthProvider.KAKAO, "kakao-profile", "테스트", now));
        org.springframework.test.util.ReflectionTestUtils.setField(user, "role", UserRole.ADMIN);
        users.saveAndFlush(user);

        for (AgreementType type : AgreementType.values()) {
            agreements.save(new UserAgreement(user.getId(), type, "1.0", true, now));
        }
        Pet active = pets.saveAndFlush(pet(user.getId(), now));
        Pet ended = pets.saveAndFlush(pet(user.getId(), now));
        Pet deleted = pets.saveAndFlush(pet(user.getId(), now));
        jdbc.update("update pets set status = 'ENDED' where id = ?", ended.getId());
        jdbc.update("update pets set status = 'DELETED' where id = ?", deleted.getId());
        entityManager.clear();

        UserProfileResult result = service.get(user.getId());

        assertThat(result.status()).isEqualTo(UserStatus.PENDING_TERMS);
        assertThat(result.role()).isEqualTo(UserRole.ADMIN);
        assertThat(result.region()).isNull();
        assertThat(result.requiredAgreements()).hasSize(3);
        assertThat(result.agreements()).hasSize(4);
        assertThat(result.needsReconsent()).isFalse();
        assertThat(result.petCount().active()).isEqualTo(1);
        assertThat(result.petCount().ended()).isEqualTo(1);
        assertThat(active.getId()).isNotNull();
    }

    @Test
    void needsReconsentWhenRequiredVersionIsMissingOrOld() {
        Instant now = Instant.parse("2026-10-19T00:00:00Z");
        User user = users.saveAndFlush(User.pending(OAuthProvider.GOOGLE, "google-profile", "테스트", now));
        agreements.saveAll(List.of(
                new UserAgreement(user.getId(), AgreementType.TERMS, "0.9", true, now),
                new UserAgreement(user.getId(), AgreementType.PRIVACY, "1.0", true, now)
        ));

        assertThat(service.get(user.getId()).needsReconsent()).isTrue();
    }

    private Pet pet(Long userId, Instant now) {
        return Pet.create(userId, UUID.randomUUID().toString(), 1L, "개체", List.of(),
                null, null, null, null, Certainty.UNKNOWN, Sex.UNKNOWN,
                Certainty.UNKNOWN, null, now);
    }
}
