package com.pabom.backend.record.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.*;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.record.domain.error.RecordErrorCode;
import com.pabom.backend.record.domain.type.*;
import com.pabom.backend.record.infrastructure.persistence.*;
import com.pabom.backend.record.presentation.request.CreateRecordRequest;
import com.pabom.backend.user.application.service.*;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:pabom-record-withdrawal-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true"})
@ActiveProfiles("test")
class RecordWithdrawalCleanupIntegrationTest {
    @Autowired WithdrawalService withdrawal; @Autowired WithdrawalCleanupService cleanup;
    @Autowired RecordService recordsService; @Autowired CareRecordJpaRepository records;
    @Autowired CareRecordRevisionJpaRepository revisions; @Autowired PetJpaRepository pets;
    @Autowired FileJpaRepository files; @Autowired UserJpaRepository users;
    @Autowired WithdrawalCleanupJobJpaRepository jobs; @Autowired Clock clock;
    @MockitoBean FileStoragePort storage;

    @Test void withdrawalPhysicallyDeletesActiveAndSoftDeletedRecordsRevisionsAndRecordPhotos() {
        Instant now=clock.instant(); User user=users.saveAndFlush(User.pending(OAuthProvider.GOOGLE,"withdraw-record-owner","owner",now));
        Pet pet=pets.saveAndFlush(Pet.create(user.getId(),UUID.randomUUID().toString(),1L,"리플",List.of(),null,null,null,null,Certainty.UNKNOWN,Sex.UNKNOWN,Certainty.UNKNOWN,null,now));
        File photo=files.saveAndFlush(File.temporary(user.getId(),FilePurpose.RECORD_PHOTO,"image/png",1,1,1,"records/withdraw.png",now));
        var created=recordsService.create(user.getId(),new CreateRecordRequest(UUID.randomUUID(),pet.getId(),"MEMO",
                OffsetDateTime.now(clock).minusHours(1),TimePrecision.EXACT,null,null,null,"삭제 대상",photo.getId(),null));
        recordsService.delete(user.getId(),created.record().id());
        assertThat(revisions.findAllByCareRecordIdOrderByIdAsc(created.record().id())).hasSize(1);

        withdrawal.withdraw(user.getId()); Long jobId=jobs.findAll().getFirst().getId(); cleanup.process(jobId); cleanup.process(jobId);

        assertThat(records.findAllByUserId(user.getId())).isEmpty();
        assertThat(revisions.findAllByCareRecordIdOrderByIdAsc(created.record().id())).isEmpty();
        assertThat(files.findAllByOwnerId(user.getId())).isEmpty();
        verify(storage).delete(photo.getStorageKey());
        assertThatThrownBy(() -> recordsService.get(user.getId(),created.record().id()))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_FOUND));
        assertThatThrownBy(() -> recordsService.restore(user.getId(),created.record().id()))
                .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_FOUND));
    }
}
