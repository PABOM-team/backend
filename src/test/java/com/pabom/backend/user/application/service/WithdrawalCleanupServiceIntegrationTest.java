package com.pabom.backend.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.pabom.backend.agreement.domain.entity.UserAgreement;
import com.pabom.backend.agreement.domain.type.AgreementType;
import com.pabom.backend.agreement.infrastructure.persistence.UserAgreementJpaRepository;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedRefreshToken;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.auth.infrastructure.token.ServiceTokenProvider;
import com.pabom.backend.event.domain.entity.LoginEvent;
import com.pabom.backend.event.infrastructure.persistence.LoginEventJpaRepository;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.domain.type.CleanupJobStatus;
import com.pabom.backend.user.infrastructure.persistence.RefreshTokenJpaRepository;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import com.pabom.backend.user.infrastructure.persistence.WithdrawalCleanupJobJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class WithdrawalCleanupServiceIntegrationTest {

    @Autowired WithdrawalService withdrawalService;
    @Autowired WithdrawalCleanupService cleanupService;
    @Autowired UserJpaRepository users;
    @Autowired RefreshTokenJpaRepository refreshTokens;
    @Autowired UserAgreementJpaRepository agreements;
    @Autowired PetJpaRepository pets;
    @Autowired FileJpaRepository files;
    @Autowired LoginEventJpaRepository events;
    @Autowired WithdrawalCleanupJobJpaRepository jobs;
    @Autowired ServiceTokenProvider tokens;
    @Autowired Clock clock;

    @MockitoBean FileStoragePort storage;

    @BeforeEach
    void cleanDatabase() {
        jobs.deleteAll();
        events.deleteAll();
        pets.deleteAll();
        files.deleteAll();
        agreements.deleteAll();
        refreshTokens.deleteAll();
        users.deleteAll();
        reset(storage);
    }

    @Test
    void retriesFailureThenDeletesAllPersonalDataAndAnonymizesEventsIdempotently() {
        Instant now = clock.instant();
        User user = User.pending(OAuthProvider.KAKAO, "cleanup-provider", "개인정보", now);
        user.activate(now);
        user = users.saveAndFlush(user);
        Long userId = user.getId();
        agreements.saveAndFlush(new UserAgreement(
                userId, AgreementType.TERMS, "1.0", true, now));
        File file = files.saveAndFlush(File.temporary(
                userId, FilePurpose.PET_PHOTO, "image/png", 100, 10, 10,
                "users/" + userId + "/pet.png", now));
        pets.saveAndFlush(Pet.create(
                userId, UUID.randomUUID().toString(), 1L, "개체", List.of(), null,
                null, null, null, Certainty.UNKNOWN, Sex.UNKNOWN,
                Certainty.UNKNOWN, file.getId(), now));
        events.saveAndFlush(LoginEvent.succeeded(userId, false, now));
        IssuedRefreshToken issued = tokens.issueRefreshToken(userId, now);
        refreshTokens.saveAndFlush(new RefreshToken(
                issued.hash(), userId, issued.familyId(), issued.issuedAt(), issued.expiresAt()));
        withdrawalService.withdraw(userId);
        Long jobId = jobs.findAll().getFirst().getId();

        doThrow(new RuntimeException("temporary storage outage"))
                .when(storage).delete(file.getStorageKey());
        cleanupService.process(jobId);

        assertThat(jobs.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(CleanupJobStatus.FAILED);
        assertThat(files.findAllByOwnerId(userId)).hasSize(1);

        reset(storage);
        cleanupService.process(jobId);
        cleanupService.process(jobId);

        assertThat(jobs.findById(jobId).orElseThrow().getStatus())
                .isEqualTo(CleanupJobStatus.COMPLETED);
        assertThat(pets.countByUserIdAndStatus(
                userId, com.pabom.backend.pet.domain.type.PetStatus.ACTIVE)).isZero();
        assertThat(files.findAllByOwnerId(userId)).isEmpty();
        assertThat(agreements.findAllByUserIdOrderByAgreedAtDesc(userId)).isEmpty();
        assertThat(refreshTokens.findAll()).isEmpty();
        assertThat(events.findAll()).singleElement()
                .extracting(LoginEvent::getUserId).isNull();
        assertThat(users.findById(userId)).isPresent();
        verify(storage, org.mockito.Mockito.atLeastOnce()).delete(file.getStorageKey());
    }
}
