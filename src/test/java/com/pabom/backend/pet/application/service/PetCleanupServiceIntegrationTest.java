package com.pabom.backend.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.CareRecord;
import com.pabom.backend.record.domain.entity.CareRecordRevision;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.domain.type.RevisionChangeType;
import com.pabom.backend.record.domain.type.TimePrecision;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.CareRecordRevisionJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-pet-cleanup-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class PetCleanupServiceIntegrationTest {

    @Autowired PetCleanupService cleanup;
    @Autowired PetJpaRepository pets;
    @Autowired CareRecordJpaRepository records;
    @Autowired CareRecordRevisionJpaRepository revisions;
    @Autowired RoutineJpaRepository routines;
    @Autowired CareItemJpaRepository items;
    @Autowired FileJpaRepository files;
    @Autowired UserJpaRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired Clock clock;
    @MockitoBean FileStoragePort storage;

    @BeforeEach
    void setUp() {
        revisions.deleteAll();
        records.deleteAll();
        routines.deleteAll();
        pets.deleteAll();
        files.deleteAll();
        users.deleteAll();
    }

    @Test
    void physicallyPurgesThirtyDayOldPetDependenciesAndStorageObjects() {
        Instant now = clock.instant();
        User owner = users.saveAndFlush(User.pending(
                OAuthProvider.GOOGLE, "cleanup-owner", "owner", now));
        File petPhoto = files.saveAndFlush(File.temporary(
                owner.getId(), FilePurpose.PET_PHOTO, "image/png", 1, 1, 1, "pet/old.png", now));
        File recordPhoto = files.saveAndFlush(File.temporary(
                owner.getId(), FilePurpose.RECORD_PHOTO, "image/png", 1, 1, 1, "record/old.png", now));
        Pet pet = pets.saveAndFlush(Pet.create(owner.getId(), UUID.randomUUID().toString(), 1L,
                "리플", List.of(), null, null, null, null, Certainty.UNKNOWN,
                Sex.UNKNOWN, Certainty.UNKNOWN, petPhoto.getId(), now));
        CareItem item = items.findByCodeAndActiveTrue("MEMO").orElseThrow();
        Routine routine = routines.saveAndFlush(Routine.active(pet.getId(), item.getId(), 7, now));
        CareRecord record = records.saveAndFlush(CareRecord.manual(owner.getId(), pet.getId(), item.getId(),
                routine.getId(), UUID.randomUUID().toString(), "hash", now, TimePrecision.EXACT,
                null, null, null, "memo", recordPhoto.getId(), now));
        revisions.saveAndFlush(CareRecordRevision.of(record.getId(), RevisionChangeType.EDIT,
                "[]", "{}", "{}", now));
        jdbc.update("update pets set deleted_at=? where id=?",
                java.sql.Timestamp.from(now.minus(Duration.ofDays(31))), pet.getId());
        entityManager.clear();

        cleanup.process(pet.getId());
        cleanup.process(pet.getId());

        assertThat(pets.findById(pet.getId())).isEmpty();
        assertThat(records.findById(record.getId())).isEmpty();
        assertThat(revisions.findAllByCareRecordIdOrderByIdAsc(record.getId())).isEmpty();
        assertThat(routines.findById(routine.getId())).isEmpty();
        assertThat(files.findById(petPhoto.getId())).isEmpty();
        assertThat(files.findById(recordPhoto.getId())).isEmpty();
        verify(storage).delete("pet/old.png");
        verify(storage).delete("record/old.png");
    }
}
