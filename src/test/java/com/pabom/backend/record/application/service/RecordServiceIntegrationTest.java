package com.pabom.backend.record.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.event.infrastructure.persistence.LoginEventJpaRepository;
import com.pabom.backend.file.application.port.FileStoragePort;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.*;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.pet.application.service.PetCommandService;
import com.pabom.backend.record.domain.entity.*;
import com.pabom.backend.record.domain.error.RecordErrorCode;
import com.pabom.backend.record.domain.type.*;
import com.pabom.backend.record.infrastructure.persistence.*;
import com.pabom.backend.record.presentation.request.*;
import com.pabom.backend.record.presentation.response.*;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:pabom-record-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE","spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true"})
@ActiveProfiles("test")
class RecordServiceIntegrationTest {
    @Autowired RecordService service; @Autowired CareRecordJpaRepository records;
    @Autowired CareRecordRevisionJpaRepository revisions; @Autowired RoutineJpaRepository routines;
    @Autowired CareItemJpaRepository items; @Autowired PetJpaRepository pets; @Autowired FileJpaRepository files;
    @Autowired UserJpaRepository users; @Autowired LoginEventJpaRepository events;
    @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc; @Autowired Clock clock;
    @Autowired EntityManager entityManager;
    @Autowired PetCommandService petCommands;
    @MockitoBean FileStoragePort storage;
    private User owner; private User other; private Pet pet;

    @BeforeEach void setup() {
        revisions.deleteAll(); records.deleteAll(); routines.deleteAll(); events.deleteAll(); pets.deleteAll(); files.deleteAll(); users.deleteAll();
        Instant now=clock.instant(); owner=users.saveAndFlush(User.pending(OAuthProvider.GOOGLE,"record-owner","owner",now));
        other=users.saveAndFlush(User.pending(OAuthProvider.GOOGLE,"record-other","other",now));
        pet=pets.saveAndFlush(Pet.create(owner.getId(),UUID.randomUUID().toString(),1L,"리플",List.of(),null,null,null,null,Certainty.UNKNOWN,Sex.UNKNOWN,Certainty.UNKNOWN,null,now));
        jdbc.update("update care_items set min_value=null,max_value=null where code='WEIGHT'");
    }

    @Test void createsAllSupportedItemsWithoutRoutineAndHandlesDateOnlyAndIdempotency() throws Exception {
        UUID key=UUID.randomUUID();
        CreateRecordRequest sameRequest=request(key,"FEEDING",FeedingOutcome.ATE_PART,null,null,"일부 먹음",TimePrecision.EXACT);
        RecordMutationResponse feeding=service.create(owner.getId(), sameRequest);
        assertThat(feeding.routineUpdated()).isNull(); assertThat(feeding.record().routineId()).isNull();
        assertThat(service.create(owner.getId(),sameRequest).record().id()).isEqualTo(feeding.record().id());
        assertError(() -> service.create(owner.getId(),request(key,"FEEDING",FeedingOutcome.ATE_ALL,null,null,"일부 먹음",TimePrecision.EXACT)),RecordErrorCode.CLIENT_REQUEST_ID_REUSED);
        RecordMutationResponse weight=service.create(owner.getId(),request(UUID.randomUUID(),"WEIGHT",null,new BigDecimal("42.5"),null,null,TimePrecision.DATE_ONLY));
        assertThat(weight.record().occurredAt().toLocalTime()).isEqualTo(LocalTime.NOON);
        service.create(owner.getId(),request(UUID.randomUUID(),"SHED",null,null,mapper.readTree("{\"stage\":\"FINISHED\",\"observedDirectly\":true}"),null,TimePrecision.EXACT));
        service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null,"상태 양호",TimePrecision.EXACT));
        assertThat(records.count()).isEqualTo(4); assertThat(events.findAll()).extracting(e->e.getEventCode()).containsOnly("EV-12");
    }

    @Test void validatesItemSpecificValuesTimesContactsAndOwnership() {
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"WEIGHT",null,null,null,null,TimePrecision.EXACT)),RecordErrorCode.VALUE_REQUIRED);
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null," ",TimePrecision.EXACT)),RecordErrorCode.MEMO_REQUIRED);
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"WEIGHT",FeedingOutcome.ATE_ALL,BigDecimal.ONE,null,null,TimePrecision.EXACT)),RecordErrorCode.OUTCOME_NOT_ALLOWED);
        CreateRecordRequest contact=request(UUID.randomUUID(),"MEMO",null,null,null,"010-1234-5678",TimePrecision.EXACT);
        assertError(() -> service.create(owner.getId(),contact),RecordErrorCode.MEMO_CONTACT_NOT_ALLOWED);
        CreateRecordRequest future=new CreateRecordRequest(UUID.randomUUID(),pet.getId(),"FEEDING",OffsetDateTime.now(clock).plusMinutes(6),null,null,null,null,null,null,null);
        assertError(() -> service.create(owner.getId(),future),RecordErrorCode.OCCURRED_AT_IN_FUTURE);
        CreateRecordRequest old=new CreateRecordRequest(UUID.randomUUID(),pet.getId(),"FEEDING",OffsetDateTime.parse("1999-12-31T00:00:00Z"),null,null,null,null,null,null,null);
        assertError(() -> service.create(owner.getId(),old),RecordErrorCode.OCCURRED_AT_TOO_OLD);
        assertThatThrownBy(() -> service.get(other.getId(),999L)).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(RecordErrorCode.RECORD_NOT_FOUND));
    }

    @Test void paginatesFiltersUpdatesDeletesRestoresAndPreservesHistory() {
        RecordMutationResponse first=service.create(owner.getId(),request(UUID.randomUUID(),"FEEDING",FeedingOutcome.ATE_PART,null,null,"a",TimePrecision.EXACT));
        service.create(owner.getId(),request(UUID.randomUUID(),"WEIGHT",null,BigDecimal.TEN,null,null,TimePrecision.EXACT));
        service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null,"c",TimePrecision.EXACT));
        RecordListResponse page1=service.list(owner.getId(),pet.getId(),null,null,LocalDate.now(clock).minusDays(2),LocalDate.now(clock),false,null,2);
        assertThat(page1.items()).hasSize(2); assertThat(page1.nextCursor()).isNotNull(); assertThat(page1.lastFeeding()).isNotNull();
        assertThat(service.list(owner.getId(),pet.getId(),null,null,null,null,false,page1.nextCursor(),2).lastFeeding()).isNull();
        assertError(() -> service.list(owner.getId(),null,null,null,LocalDate.now(clock).minusDays(366),LocalDate.now(clock),false,null,20),com.pabom.backend.auth.domain.error.AuthErrorCode.VALIDATION_FAILED);
        UpdateRecordRequest update=new UpdateRecordRequest(); update.setRevisionNo(1); update.setMemo("수정");
        RecordMutationResponse edited=service.update(owner.getId(),first.record().id(),update);
        assertThat(edited.record().revisionNo()).isEqualTo(2);
        assertError(() -> service.update(owner.getId(),first.record().id(),update),RecordErrorCode.RECORD_CONFLICT);
        DeleteRecordResponse deleted=service.delete(owner.getId(),first.record().id()); service.delete(owner.getId(),first.record().id());
        assertThat(revisions.countByCareRecordIdAndChangeType(first.record().id(),RevisionChangeType.DELETE)).isEqualTo(1);
        assertThat(service.get(owner.getId(),first.record().id()).record().deletedAt())
                .isCloseTo(deleted.deletedAt(), within(1, java.time.temporal.ChronoUnit.MICROS));
        RecordMutationResponse restored=service.restore(owner.getId(),first.record().id()); assertThat(restored.record().deletedAt()).isNull();
        assertThat(service.get(owner.getId(),first.record().id()).revisions()).extracting(RevisionResponse::changeType).containsExactly(RevisionChangeType.EDIT,RevisionChangeType.DELETE,RevisionChangeType.RESTORE);
        assertError(() -> service.restore(owner.getId(),first.record().id()),RecordErrorCode.RECORD_NOT_DELETED);
    }

    @Test void linksExistingRoutineRecalculatesDueDateAndReturnsPhotoUrl() {
        CareItem feeding=items.findByCodeAndActiveTrue("FEEDING").orElseThrow();
        Routine routine=routines.saveAndFlush(Routine.active(pet.getId(),feeding.getId(),7,clock.instant()));
        File photo=files.saveAndFlush(File.temporary(owner.getId(),FilePurpose.RECORD_PHOTO,"image/png",1,1,1,"record/photo.png",clock.instant()));
        given(storage.createDownloadUrl(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any())).willReturn("https://example.test/photo");
        CreateRecordRequest request=new CreateRecordRequest(UUID.randomUUID(),pet.getId(),"FEEDING",OffsetDateTime.now(clock).minusHours(1),TimePrecision.EXACT,FeedingOutcome.ATE_ALL,null,null,null,photo.getId(),100);
        RecordMutationResponse result=service.create(owner.getId(),request);
        assertThat(result.record().routineId()).isEqualTo(routine.getId()); assertThat(result.routineUpdated().nextDueAt()).isEqualTo(result.record().occurredAt().plusDays(7));
        assertThat(result.record().photo().url()).isEqualTo("https://example.test/photo");
        service.delete(owner.getId(),result.record().id()); assertThat(routines.findById(routine.getId()).orElseThrow().getNextDueAt()).isNull();
    }

    @Test void enforcesDeletedInactiveExpiredImmutableRangeAndFileErrors() {
        var created=service.create(owner.getId(),request(UUID.randomUUID(),"FEEDING",FeedingOutcome.ATE_ALL,null,null,null,TimePrecision.EXACT));
        service.delete(owner.getId(),created.record().id());
        UpdateRecordRequest deletedUpdate=new UpdateRecordRequest(); deletedUpdate.setRevisionNo(1); deletedUpdate.setMemo("x");
        assertError(() -> service.update(owner.getId(),created.record().id(),deletedUpdate),RecordErrorCode.RECORD_DELETED);
        jdbc.update("update care_records set deleted_at=? where id=?",java.sql.Timestamp.from(clock.instant().minus(Duration.ofDays(31))),created.record().id()); entityManager.clear();
        assertError(() -> service.restore(owner.getId(),created.record().id()),RecordErrorCode.RESTORE_WINDOW_EXPIRED);
        assertError(() -> service.get(owner.getId(),created.record().id()),RecordErrorCode.RECORD_NOT_FOUND);

        UpdateRecordRequest immutable;
        try { immutable=mapper.readValue("{\"revisionNo\":1,\"petId\":999}",UpdateRecordRequest.class); }
        catch (Exception exception) { throw new AssertionError(exception); }
        var editable=service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null,"ok",TimePrecision.EXACT));
        assertError(() -> service.update(owner.getId(),editable.record().id(),immutable),RecordErrorCode.FIELD_NOT_EDITABLE);
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"UNKNOWN",null,null,null,null,TimePrecision.EXACT)),RecordErrorCode.CARE_ITEM_INVALID);
        CreateRecordRequest missingFile=new CreateRecordRequest(UUID.randomUUID(),pet.getId(),"MEMO",OffsetDateTime.now(clock).minusHours(1),TimePrecision.EXACT,null,null,null,"ok",99999L,null);
        assertError(() -> service.create(owner.getId(),missingFile),com.pabom.backend.file.domain.error.FileErrorCode.FILE_NOT_FOUND);

        jdbc.update("update care_items set min_value=1,max_value=10 where code='WEIGHT'"); entityManager.clear();
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"WEIGHT",null,new BigDecimal("11"),null,null,TimePrecision.EXACT)),RecordErrorCode.VALUE_OUT_OF_RANGE);
        jdbc.update("update pets set status='ENDED' where id=?",pet.getId()); entityManager.clear();
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null,"ok",TimePrecision.EXACT)),RecordErrorCode.PET_NOT_ACTIVE);
    }

    @Test void petDeletionSoftDeletesOnlyActiveRecordsAndBlocksEveryRecordAccessPath() {
        var active=service.create(owner.getId(),request(UUID.randomUUID(),"FEEDING",FeedingOutcome.ATE_ALL,null,null,null,TimePrecision.EXACT));
        var alreadyDeleted=service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null,"old",TimePrecision.EXACT));
        service.delete(owner.getId(),alreadyDeleted.record().id());
        Instant originalDeletedAt=records.findById(alreadyDeleted.record().id()).orElseThrow().getDeletedAt();

        petCommands.delete(owner.getId(),pet.getId());
        entityManager.clear();

        Pet deletedPet=pets.findById(pet.getId()).orElseThrow();
        assertThat(deletedPet.getDeletedAt()).isNotNull();
        assertThat(records.findById(active.record().id()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(records.findById(alreadyDeleted.record().id()).orElseThrow().getDeletedAt()).isEqualTo(originalDeletedAt);
        assertThat(revisions.countByCareRecordIdAndChangeType(active.record().id(),RevisionChangeType.DELETE)).isZero();
        assertThat(revisions.countByCareRecordIdAndChangeType(alreadyDeleted.record().id(),RevisionChangeType.DELETE)).isEqualTo(1);
        assertThat(service.list(owner.getId(),null,null,null,null,null,true,null,20).items()).isEmpty();
        assertError(() -> service.get(owner.getId(),active.record().id()),RecordErrorCode.RECORD_NOT_FOUND);
        assertError(() -> service.restore(owner.getId(),alreadyDeleted.record().id()),RecordErrorCode.RECORD_NOT_FOUND);
        assertError(() -> service.create(owner.getId(),request(UUID.randomUUID(),"MEMO",null,null,null,"new",TimePrecision.EXACT)),
                com.pabom.backend.pet.domain.error.PetErrorCode.PET_NOT_FOUND);
    }

    private CreateRecordRequest request(UUID id,String code,FeedingOutcome outcome,BigDecimal value,com.fasterxml.jackson.databind.JsonNode detail,String memo,TimePrecision precision) {
        return new CreateRecordRequest(id,pet.getId(),code,OffsetDateTime.now(clock).minusHours(1),precision,outcome,value,detail,memo,null,100);
    }
    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action,Object code) { assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.errorCode()).isEqualTo(code)); }
}
