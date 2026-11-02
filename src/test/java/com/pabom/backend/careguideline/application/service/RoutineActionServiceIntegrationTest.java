package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.*;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.careguideline.application.result.*;
import com.pabom.backend.careguideline.presentation.request.*;
import com.pabom.backend.event.infrastructure.persistence.LoginEventJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.type.*;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.record.domain.entity.*;
import com.pabom.backend.record.domain.error.*;
import com.pabom.backend.record.domain.type.*;
import com.pabom.backend.record.infrastructure.persistence.*;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-routine-action-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class RoutineActionServiceIntegrationTest {
    @Autowired RoutineActionService service;
    @Autowired TodayQueryService todayService;
    @Autowired RoutineJpaRepository routines;
    @Autowired CareRecordJpaRepository records;
    @Autowired RoutinePostponementEventJpaRepository postponements;
    @Autowired CareItemJpaRepository items;
    @Autowired PetJpaRepository pets;
    @Autowired UserJpaRepository users;
    @Autowired LoginEventJpaRepository loginEvents;
    @Autowired RoutineDueDateCalculator dueDates;
    @Autowired Clock clock;

    private User owner;
    private User other;
    private Pet pet;
    private Routine routine;
    private LocalDate today;

    @BeforeEach
    void setup() {
        postponements.deleteAll();
        records.deleteAll();
        routines.deleteAll();
        loginEvents.deleteAll();
        pets.deleteAll();
        users.deleteAll();
        Instant now = clock.instant();
        owner = users.saveAndFlush(User.pending(OAuthProvider.GOOGLE, "routine-action-owner", "owner", now));
        other = users.saveAndFlush(User.pending(OAuthProvider.GOOGLE, "routine-action-other", "other", now));
        pet = pets.saveAndFlush(Pet.create(owner.getId(), UUID.randomUUID().toString(), 1L, "레오",
                List.of(), null, null, null, null, Certainty.UNKNOWN, Sex.UNKNOWN,
                Certainty.UNKNOWN, null, now));
        CareItem feeding = items.findByCodeAndActiveTrue("FEEDING").orElseThrow();
        routine = Routine.active(pet.getId(), feeding.getId(), 3, now);
        today = LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul")));
        routine.updateNextDueAt(dueDates.toDateOnlyInstant(today.minusDays(2)), now);
        routine = routines.saveAndFlush(routine);
    }

    @Test
    void completesFromActualDateClearsPostponementAndIsIdempotent() {
        PostponeRoutineRequest postpone = postpone(today.plusDays(2));
        service.postpone(owner.getId(), routine.getId(), postpone);
        UUID requestId = UUID.randomUUID();
        OffsetDateTime occurredAt = OffsetDateTime.ofInstant(clock.instant().minusSeconds(60), ZoneId.of("Asia/Seoul"));
        CompleteRoutineRequest request = new CompleteRoutineRequest(requestId, occurredAt,
                TimePrecision.EXACT, FeedingOutcome.REFUSED, null, null, null);

        RoutineActionResult result = service.complete(owner.getId(), routine.getId(), request);
        RoutineActionResult replay = service.complete(owner.getId(), routine.getId(), request);
        LocalDate completedOn = occurredAt.toLocalDate();

        assertThat(replay.record().id()).isEqualTo(result.record().id());
        assertThat(result.record().outcome()).isEqualTo(FeedingOutcome.REFUSED);
        assertThat(result.routine().lastDoneOn()).isEqualTo(completedOn);
        assertThat(result.routine().nextDueOn()).isEqualTo(completedOn.plusDays(3));
        Routine stored = routines.findById(routine.getId()).orElseThrow();
        assertThat(stored.getPostponedBaseDue()).isNull();
        assertThat(stored.getPostponedAt()).isNull();
        assertThat(records.count()).isEqualTo(1);
    }

    @Test
    void rejectsDifferentUuidDuplicateAndOtherOwner() {
        LocalDate actionDate = today.minusDays(1);
        service.skip(owner.getId(), routine.getId(),
                new SkipRoutineRequest(UUID.randomUUID(), actionDate, SkipReasonCode.SCHEDULE, "일정"));

        assertError(() -> service.complete(owner.getId(), routine.getId(),
                new CompleteRoutineRequest(UUID.randomUUID(), actionDate.atTime(12, 0)
                        .atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime(), TimePrecision.DATE_ONLY,
                        FeedingOutcome.ATE_ALL, null, null, null)), RoutineErrorCode.ROUTINE_ACTION_DUPLICATE);
        assertError(() -> service.skip(other.getId(), routine.getId(),
                new SkipRoutineRequest(UUID.randomUUID(), today, null, null)), RoutineErrorCode.ROUTINE_NOT_FOUND);
    }

    @Test
    void missingFeedingOutcomeIsStoredAsNeedsResult() {
        RoutineActionResult result = service.complete(owner.getId(), routine.getId(),
                new CompleteRoutineRequest(UUID.randomUUID(), OffsetDateTime.ofInstant(
                        clock.instant().minusSeconds(60), ZoneId.of("Asia/Seoul")),
                        TimePrecision.EXACT, null, null, null, null));

        assertThat(result.todayStatus()).isEqualTo(TodayTaskStatus.NEEDS_RESULT);
        assertThat(result.record().outcome()).isNull();
    }

    @Test
    void postponesRepeatedlyWithoutChangingCycleAndCancelRestoresFirstDueDate() {
        int interval = routine.getIntervalDays();
        CycleSource source = routine.getCycleSource();
        LocalDate base = today.minusDays(2);

        service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(35)));
        RoutinePostponeResult second = service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(40)));
        RoutinePostponeResult cancelled = service.postpone(owner.getId(), routine.getId(), postpone(null));

        assertThat(second.routine().baseDueOn()).isEqualTo(base);
        assertThat(second.routine().nextDueOn()).isEqualTo(today.plusDays(40));
        assertThat(cancelled.routine().nextDueOn()).isEqualTo(base);
        assertThat(cancelled.routine().baseDueOn()).isNull();
        assertThat(postponements.findAllByRoutineIdOrderByIdAsc(routine.getId())).hasSize(3)
                .extracting(RoutinePostponementEvent::getEventType)
                .containsExactly(PostponementEventType.POSTPONED, PostponementEventType.POSTPONED,
                        PostponementEventType.CANCELLED);
        Routine stored = routines.findById(routine.getId()).orElseThrow();
        assertThat(stored.getIntervalDays()).isEqualTo(interval);
        assertThat(stored.getCycleSource()).isEqualTo(source);
    }

    @Test
    void blocksPostponementCancellationAfterOccurrenceFinishesCycle() {
        service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(2)));
        service.skip(owner.getId(), routine.getId(), new SkipRoutineRequest(UUID.randomUUID(), today, null, null));

        assertError(() -> service.postpone(owner.getId(), routine.getId(), postpone(null)),
                RoutineErrorCode.POSTPONEMENT_CYCLE_COMPLETED);
    }

    @Test
    void todayShowsOnlyIncompleteDueDatesFromZeroThroughSixDaysAndUsesPostponedDate() {
        Routine stored = routines.findById(routine.getId()).orElseThrow();
        stored.updateNextDueAt(dueDates.toDateOnlyInstant(today.minusDays(6)), clock.instant());
        routines.saveAndFlush(stored);
        TodayResult sixDays = todayService.get(owner.getId(), pet.getId(), today);
        assertThat(sixDays.summary().overdueCount()).isEqualTo(1);
        assertThat(sixDays.pets().getFirst().tasks().getFirst().overdueDays()).isEqualTo(6);

        stored.updateNextDueAt(dueDates.toDateOnlyInstant(today.minusDays(7)), clock.instant());
        routines.saveAndFlush(stored);
        TodayResult sevenDays = todayService.get(owner.getId(), pet.getId(), today);
        assertThat(sevenDays.pets()).isEmpty();
        assertThat(sevenDays.summary().overdueCount()).isZero();
        assertThat(routines.findById(routine.getId()).orElseThrow().getNextDueAt())
                .isEqualTo(dueDates.toDateOnlyInstant(today.minusDays(7)));

        service.postpone(owner.getId(), routine.getId(), postpone(today));
        TodayResult postponed = todayService.get(owner.getId(), pet.getId(), today);
        assertThat(postponed.summary().dueCount()).isEqualTo(1);
        assertThat(postponed.pets().getFirst().tasks().getFirst().status()).isEqualTo(TodayTaskStatus.DUE);
    }

    private PostponeRoutineRequest postpone(LocalDate date) {
        PostponeRoutineRequest request = new PostponeRoutineRequest();
        request.setPostponeTo(date);
        return request;
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, Object code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(code));
    }
}
