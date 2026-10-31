package com.pabom.backend.careguideline.application.service;

import static org.assertj.core.api.Assertions.*;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.careguideline.application.command.CareGuidelineImportFiles;
import com.pabom.backend.careguideline.application.result.PetRoutineResult;
import com.pabom.backend.careguideline.domain.type.CareGuidelineImportMode;
import com.pabom.backend.careguideline.domain.entity.CareGuidelineVersion;
import com.pabom.backend.careguideline.domain.error.CareGuidelineQueryErrorCode;
import com.pabom.backend.careguideline.infrastructure.persistence.CareGuidelineVersionJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.application.command.CreatePetCommand;
import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.application.service.PetCommandService;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.record.domain.entity.CareItem;
import com.pabom.backend.record.domain.entity.Routine;
import com.pabom.backend.record.domain.type.CycleSource;
import com.pabom.backend.record.infrastructure.persistence.CareItemJpaRepository;
import com.pabom.backend.record.infrastructure.persistence.RoutineJpaRepository;
import com.pabom.backend.species.domain.entity.LifeStage;
import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.infrastructure.persistence.LifeStageJpaRepository;
import com.pabom.backend.species.infrastructure.persistence.SpeciesJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.UserRole;
import com.pabom.backend.user.domain.type.UserStatus;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-guideline-pr2-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Import(CareGuidelinePr2IntegrationTest.FixedClockConfig.class)
@Transactional
class CareGuidelinePr2IntegrationTest {
    private static final Path CSV_DIR = Path.of("src/docs/care-guidelines/v1_2");
    private static final Instant NOW = Instant.parse("2026-11-01T03:00:00Z");

    @Autowired CareGuidelineImportService importService;
    @Autowired SpeciesCareStandardsQueryService standardsQueryService;
    @Autowired PetRoutineQueryService routineQueryService;
    @Autowired PetCommandService petCommandService;
    @Autowired UserRepository users;
    @Autowired SpeciesJpaRepository species;
    @Autowired LifeStageJpaRepository stages;
    @Autowired RoutineJpaRepository routines;
    @Autowired CareItemJpaRepository items;
    @Autowired CareGuidelineVersionJpaRepository versions;
    @Autowired ApplicableCareGuidelineService applicableGuidelines;

    User owner;
    User other;
    Species leopard;

    @BeforeEach
    void setUp() throws Exception {
        User admin = user("admin", UserRole.ADMIN);
        owner = user("owner", UserRole.USER);
        other = user("other", UserRole.USER);
        importService.importCsv(admin.getId(), "1.2", CareGuidelineImportMode.COMMIT,
                true, LocalDate.of(2026, 11, 1), files());
        leopard = species.findByCode("LEOPARD").orElseThrow();
    }

    @Test
    void returnsPublishedStandardsForBothStagesAndUnknownFallbackWithCommonDescription() {
        var result = standardsQueryService.get(leopard.getId());

        assertThat(result.guidelineVersion()).isEqualTo("1.2");
        assertThat(result.lifeStages()).extracting(stage -> stage.lifeStage())
                .containsExactly("JUVENILE", "ADULT", "UNKNOWN");
        assertThat(result.lifeStages().get(2).appliedLifeStage()).isEqualTo("JUVENILE");
        assertThat(result.lifeStages().getFirst().careItems())
                .filteredOn(item -> item.careItemCode().equals("WEIGHT"))
                .singleElement().extracting(item -> item.description()).isNotNull();
    }

    @Test
    void reportsUnknownSpecies() {
        assertThatThrownBy(() -> standardsQueryService.get(999999L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void excludesDraftAndFutureVersions() {
        versions.save(CareGuidelineVersion.create("1.1", true,
                LocalDate.of(2026, 10, 1), owner.getId(), NOW));
        versions.save(CareGuidelineVersion.create("1.3-draft", false,
                LocalDate.of(2026, 10, 1), owner.getId(), NOW));
        versions.save(CareGuidelineVersion.create("2.0", true,
                LocalDate.of(2026, 11, 2), owner.getId(), NOW));

        assertThat(applicableGuidelines.requireApplicable().getVersionLabel()).isEqualTo("1.2");
    }

    @Test
    void rejectsPublishedVersionsWithSameEffectiveDate() {
        versions.save(CareGuidelineVersion.create("1.2-conflict", true,
                LocalDate.of(2026, 11, 1), owner.getId(), NOW));

        assertThatThrownBy(applicableGuidelines::requireApplicable)
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(
                                CareGuidelineQueryErrorCode.GUIDELINE_VERSION_CONFLICT));
    }

    @Test
    void createsUnknownRoutinesOnceAndMakesWeightDueOnRegistrationDay() {
        UUID requestId = UUID.randomUUID();
        PetResult created = petCommandService.create(owner.getId(), command(requestId, null, Certainty.UNKNOWN));
        PetResult replay = petCommandService.create(owner.getId(), command(requestId, null, Certainty.UNKNOWN));
        List<Routine> saved = routines.findAllByPetIdOrderByIdAsc(created.id());

        assertThat(replay.id()).isEqualTo(created.id());
        assertThat(saved).isNotEmpty().allMatch(routine -> routine.getCycleSource() == CycleSource.GUIDELINE)
                .allMatch(routine -> routine.getGuidelineVersionId().equals(
                        versions.findByVersionLabel("1.2").orElseThrow().getId()));
        assertThat(saved).anyMatch(routine -> !routine.isActive());
        Routine weight = saved.stream().filter(routine ->
                items.findById(routine.getCareItemId()).orElseThrow().getCode().equals("WEIGHT"))
                .findFirst().orElseThrow();
        assertThat(weight.getNextDueAt()).isEqualTo(NOW);
        assertThat(saved).filteredOn(routine -> !routine.getId().equals(weight.getId()))
                .allMatch(routine -> routine.getNextDueAt() == null);
    }

    @Test
    void appliesAdultDefaultsAndReportsOutOfRange() {
        Long adultId = stage("ADULT").getId();
        PetResult pet = petCommandService.create(owner.getId(), command(UUID.randomUUID(), adultId, Certainty.CONFIRMED));
        Routine feed = routines.findAllByPetIdOrderByIdAsc(pet.id()).stream()
                .filter(routine -> items.findById(routine.getCareItemId()).orElseThrow().getCode().equals("FEED"))
                .findFirst().orElseThrow();
        assertThat(feed.getIntervalDays()).isEqualTo(3);
        ReflectionTestUtils.setField(feed, "intervalDays", 99);
        ReflectionTestUtils.setField(feed, "cycleSource", CycleSource.USER_CUSTOM);
        routines.saveAndFlush(feed);

        PetRoutineResult result = routineQueryService.get(owner.getId(), pet.id()).stream()
                .filter(routine -> routine.careItemCode().equals("FEED")).findFirst().orElseThrow();
        assertThat(result.defaultCycleDays()).isEqualTo(3);
        assertThat(result.isOutOfRange()).isTrue();
        assertThat(result.cycleSource()).isEqualTo(CycleSource.USER_CUSTOM);
    }

    @Test
    void normalizesLegacyCodeAndDoesNotExposeDuplicateMeaning() {
        PetResult pet = petCommandService.create(owner.getId(), command(UUID.randomUUID(), stage("JUVENILE").getId(), Certainty.CONFIRMED));
        CareItem legacy = items.findByCode("FEEDING").orElseThrow();
        routines.saveAndFlush(Routine.active(pet.id(), legacy.getId(), 7, NOW));

        assertThat(routineQueryService.get(owner.getId(), pet.id()))
                .filteredOn(routine -> routine.careItemCode().equals("FEED"))
                .hasSize(1);
    }

    @Test
    void hidesOtherUsersAndSoftDeletedPets() {
        PetResult pet = petCommandService.create(owner.getId(), command(UUID.randomUUID(), null, Certainty.UNKNOWN));
        assertThatThrownBy(() -> routineQueryService.get(other.getId(), pet.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));

        petCommandService.delete(owner.getId(), pet.id());
        assertThatThrownBy(() -> routineQueryService.get(owner.getId(), pet.id()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));
    }

    @Test
    void keepsDeletedPetRoutinesAndCreatesFreshRoutinesForReregistration() {
        PetResult deleted = petCommandService.create(owner.getId(), command(UUID.randomUUID(), null, Certainty.UNKNOWN));
        int originalRoutineCount = routines.findAllByPetIdOrderByIdAsc(deleted.id()).size();
        petCommandService.delete(owner.getId(), deleted.id());

        PetResult replacement = petCommandService.create(owner.getId(), command(UUID.randomUUID(), null, Certainty.UNKNOWN));

        assertThat(routines.findAllByPetIdOrderByIdAsc(deleted.id())).hasSize(originalRoutineCount);
        assertThat(routines.findAllByPetIdOrderByIdAsc(replacement.id())).hasSize(originalRoutineCount);
    }

    private CreatePetCommand command(UUID requestId, Long stageId, Certainty certainty) {
        return new CreatePetCommand(requestId, leopard.getId(), "테스트", List.of(), null,
                LocalDate.of(2026, 1, 1), null, stageId, certainty,
                Sex.UNKNOWN, Certainty.UNKNOWN, null);
    }

    private LifeStage stage(String code) {
        return stages.findByCode(code).orElseThrow();
    }

    private User user(String id, UserRole role) {
        User user = User.pending(OAuthProvider.GOOGLE, id + UUID.randomUUID(), id, NOW);
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "status", UserStatus.ACTIVE);
        return users.save(user);
    }

    private CareGuidelineImportFiles files() throws IOException {
        return new CareGuidelineImportFiles(
                file("stages", "pabom_v1_2_stages_FINAL.csv"),
                file("items", "pabom_v1_2_items_FINAL.csv"),
                file("standards", "pabom_v1_2_standards_FINAL.csv"),
                file("stageThresholds", "pabom_v1_2_stage_thresholds_FINAL.csv"),
                file("descriptions", "pabom_v1_2_descriptions_FINAL.csv"));
    }

    private MockMultipartFile file(String part, String name) throws IOException {
        return new MockMultipartFile(part, name, "text/csv", Files.readAllBytes(CSV_DIR.resolve(name)));
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
