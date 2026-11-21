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
import com.pabom.backend.record.application.service.RecordService;
import com.pabom.backend.record.infrastructure.persistence.*;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-routine-action-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Import(RoutineActionServiceIntegrationTest.FixedClockConfig.class)
class RoutineActionServiceIntegrationTest {
    private static final Instant FIXED_NOW = Instant.parse("2026-11-21T03:00:00Z");

    @Autowired RoutineActionService service;
    @Autowired RecordService recordService;
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
    @Autowired AdjustableClock adjustableClock;

    private User owner;
    private User other;
    private Pet pet;
    private Routine routine;
    private LocalDate today;

    @BeforeEach
    void setup() {
        adjustableClock.setInstant(FIXED_NOW);
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
        assertThat(todayTask().originalDueDate()).isNull();
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
    void activeCompletionBlocksSkipOnTheSameDate() {
        service.complete(owner.getId(), routine.getId(), completeRequest(today));

        assertError(() -> service.skip(owner.getId(), routine.getId(),
                new SkipRoutineRequest(UUID.randomUUID(), today, null, null)),
                RoutineErrorCode.ROUTINE_ACTION_DUPLICATE);
    }

    @Test
    void completedThenCancelledCanBeSkippedOnTheSameDate() {
        replaceWithStartedRoutine();
        RoutineActionResult completed = service.complete(owner.getId(), routine.getId(), completeRequest(today));

        recordService.delete(owner.getId(), completed.record().id());

        Routine cancelled = routines.findById(routine.getId()).orElseThrow();
        assertThat(dueDates.toServiceDate(cancelled.getNextDueAt())).isEqualTo(today.minusDays(2));
        assertThat(todayTask().status()).isEqualTo(TodayTaskStatus.OVERDUE);

        RoutineActionResult skipped = service.skip(owner.getId(), routine.getId(),
                new SkipRoutineRequest(UUID.randomUUID(), today, SkipReasonCode.SCHEDULE, null));

        assertThat(skipped.routine().nextDueOn()).isEqualTo(today.plusDays(3));
        assertThat(todayTask()).satisfies(task -> {
            assertThat(task.status()).isEqualTo(TodayTaskStatus.SKIPPED);
            assertThat(task.originalDueDate()).isNull();
        });
        assertThat(records.findAll()).hasSize(2);
        assertThat(records.findById(completed.record().id()).orElseThrow().getDeletedAt()).isNotNull();
    }

    @Test
    void skippedThenCancelledCanBeCompletedOnTheSameDate() {
        replaceWithStartedRoutine();
        RoutineActionResult skipped = service.skip(owner.getId(), routine.getId(),
                new SkipRoutineRequest(UUID.randomUUID(), today, null, null));

        recordService.delete(owner.getId(), skipped.record().id());
        RoutineActionResult completed = service.complete(owner.getId(), routine.getId(), completeRequest(today));

        assertThat(completed.routine().lastDoneOn()).isEqualTo(today);
        assertThat(completed.routine().nextDueOn()).isEqualTo(today.plusDays(3));
        assertThat(todayTask().status()).isEqualTo(TodayTaskStatus.DONE);
    }

    @Test
    void completedThenCancelledCanBeCompletedAgainOnTheSameDate() {
        replaceWithStartedRoutine();
        RoutineActionResult first = service.complete(owner.getId(), routine.getId(), completeRequest(today));

        recordService.delete(owner.getId(), first.record().id());
        RoutineActionResult second = service.complete(owner.getId(), routine.getId(), completeRequest(today));

        assertThat(second.record().id()).isNotEqualTo(first.record().id());
        assertThat(second.routine().lastDoneOn()).isEqualTo(today);
        assertThat(second.routine().nextDueOn()).isEqualTo(today.plusDays(3));
        assertThat(todayTask().status()).isEqualTo(TodayTaskStatus.DONE);
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

        service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(10)));
        RoutinePostponeResult second = service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(20)));

        assertThat(second.routine().baseDueOn()).isEqualTo(base);
        assertThat(second.routine().nextDueOn()).isEqualTo(today.plusDays(20));
        assertThat(todayService.get(owner.getId(), pet.getId(), today.plusDays(20))
                .pets().getFirst().tasks().getFirst().originalDueDate()).isEqualTo(base);
        RoutinePostponeResult cancelled = service.postpone(owner.getId(), routine.getId(), postpone(null));
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
        adjustableClock.advance(Duration.ofSeconds(1));
        service.skip(owner.getId(), routine.getId(), new SkipRoutineRequest(UUID.randomUUID(), today, null, null));

        assertError(() -> service.postpone(owner.getId(), routine.getId(), postpone(null)),
                RoutineErrorCode.POSTPONEMENT_CYCLE_COMPLETED);
    }

    @Test
    void rejectsPostponementToTodayWithoutChangingRoutineOrHistory() {
        assertInvalidPostponeWithoutChanges(today);
    }

    @Test
    void rejectsPostponementToPastDateWithoutChangingRoutineOrHistory() {
        assertInvalidPostponeWithoutChanges(today.minusDays(1));
    }

    @Test
    void postponesToTomorrow() {
        RoutinePostponeResult result = service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(1)));

        assertThat(result.routine().nextDueOn()).isEqualTo(today.plusDays(1));
        assertThat(postponements.findAllByRoutineIdOrderByIdAsc(routine.getId())).hasSize(1);
    }

    @Test
    void postponesToTwentyNineDaysAfterToday() {
        RoutinePostponeResult result = service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(29)));

        assertThat(result.routine().nextDueOn()).isEqualTo(today.plusDays(29));
    }

    @Test
    void rejectsPostponementToThirtyDaysAfterTodayWithoutChangingRoutineOrHistory() {
        assertInvalidPostponeWithoutChanges(today.plusDays(30));
    }

    @Test
    void rejectsPostponementToThirtyOneDaysAfterTodayWithoutChangingRoutineOrHistory() {
        assertInvalidPostponeWithoutChanges(today.plusDays(31));
    }

    @Test
    void rejectsPostponementToCurrentDueDate() {
        updateCurrentDue(today.plusDays(5));

        assertInvalidPostponeWithoutChanges(today.plusDays(5));
    }

    @Test
    void rejectsPostponementBeforeCurrentDueDate() {
        updateCurrentDue(today.plusDays(5));

        assertInvalidPostponeWithoutChanges(today.plusDays(1));
    }

    @Test
    void appliesDateRangeValidationToRepeatedPostponement() {
        service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(10)));
        LocalDate dueBeforeFailure = dueDates.toServiceDate(
                routines.findById(routine.getId()).orElseThrow().getNextDueAt());
        long historyCountBeforeFailure = postponements.count();

        assertError(() -> service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(30))),
                RoutineErrorCode.POSTPONE_DATE_INVALID);

        assertThat(dueDates.toServiceDate(routines.findById(routine.getId()).orElseThrow().getNextDueAt()))
                .isEqualTo(dueBeforeFailure);
        assertThat(postponements.count()).isEqualTo(historyCountBeforeFailure);
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

        service.postpone(owner.getId(), routine.getId(), postpone(today.plusDays(1)));
        TodayResult postponed = todayService.get(owner.getId(), pet.getId(), today.plusDays(1));
        assertThat(postponed.summary().dueCount()).isEqualTo(1);
        assertThat(postponed.pets().getFirst().tasks().getFirst()).satisfies(task -> {
            assertThat(task.status()).isEqualTo(TodayTaskStatus.DUE);
            assertThat(task.originalDueDate()).isEqualTo(today.minusDays(7));
        });
    }

    private PostponeRoutineRequest postpone(LocalDate date) {
        PostponeRoutineRequest request = new PostponeRoutineRequest();
        request.setPostponeTo(date);
        return request;
    }

    private void assertInvalidPostponeWithoutChanges(LocalDate postponeTo) {
        Routine before = routines.findById(routine.getId()).orElseThrow();
        Instant nextDueBefore = before.getNextDueAt();
        LocalDate baseDueBefore = before.getPostponedBaseDue();
        long historyCountBefore = postponements.count();

        assertError(() -> service.postpone(owner.getId(), routine.getId(), postpone(postponeTo)),
                RoutineErrorCode.POSTPONE_DATE_INVALID);

        Routine after = routines.findById(routine.getId()).orElseThrow();
        assertThat(after.getNextDueAt()).isEqualTo(nextDueBefore);
        assertThat(after.getPostponedBaseDue()).isEqualTo(baseDueBefore);
        assertThat(postponements.count()).isEqualTo(historyCountBefore);
    }

    private void updateCurrentDue(LocalDate currentDue) {
        Routine stored = routines.findById(routine.getId()).orElseThrow();
        stored.updateNextDueAt(dueDates.toDateOnlyInstant(currentDue), clock.instant());
        routine = routines.saveAndFlush(stored);
    }

    private CompleteRoutineRequest completeRequest(LocalDate date) {
        return new CompleteRoutineRequest(UUID.randomUUID(), date.atTime(12, 0)
                .atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime(), TimePrecision.DATE_ONLY,
                FeedingOutcome.ATE_ALL, null, null, null);
    }

    private void replaceWithStartedRoutine() {
        routines.deleteAll();
        CareItem feeding = items.findByCodeAndActiveTrue("FEEDING").orElseThrow();
        LocalDate startDate = today.minusDays(2);
        routine = routines.saveAndFlush(Routine.guideline(pet.getId(), feeding.getId(), 3, true,
                null, startDate, dueDates.toDateOnlyInstant(startDate), clock.instant()));
    }

    private TodayResult.Task todayTask() {
        return todayService.get(owner.getId(), pet.getId(), today).pets().stream()
                .flatMap(group -> group.tasks().stream())
                .filter(task -> task.routineId().equals(routine.getId()))
                .findFirst().orElseThrow();
    }

    private void assertError(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, Object code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.errorCode()).isEqualTo(code));
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        AdjustableClock fixedClock() {
            return new AdjustableClock(FIXED_NOW, ZoneOffset.UTC);
        }
    }

    static class AdjustableClock extends Clock {
        private Clock delegate;

        AdjustableClock(Instant instant, ZoneId zone) {
            delegate = Clock.fixed(instant, zone);
        }

        void setInstant(Instant instant) {
            delegate = Clock.fixed(instant, delegate.getZone());
        }

        void advance(Duration duration) {
            delegate = Clock.offset(delegate, duration);
        }

        @Override
        public ZoneId getZone() {
            return delegate.getZone();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return delegate.withZone(zone);
        }

        @Override
        public Instant instant() {
            return delegate.instant();
        }
    }
}
