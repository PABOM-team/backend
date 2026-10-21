package com.pabom.backend.pet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.file.infrastructure.persistence.FileJpaRepository;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.application.command.CreatePetCommand;
import com.pabom.backend.pet.application.command.PatchValue;
import com.pabom.backend.pet.application.command.UpdatePetCommand;
import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.application.result.PetListResult;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.pet.infrastructure.persistence.PetJpaRepository;
import com.pabom.backend.species.domain.entity.Morph;
import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.domain.error.SpeciesErrorCode;
import com.pabom.backend.species.infrastructure.persistence.LifeStageJpaRepository;
import com.pabom.backend.species.infrastructure.persistence.MorphJpaRepository;
import com.pabom.backend.species.infrastructure.persistence.SpeciesJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pabom-pet-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
class PetServiceIntegrationTest {

    @Autowired private PetCommandService commandService;
    @Autowired private PetQueryService queryService;
    @Autowired private PetJpaRepository petRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private SpeciesJpaRepository speciesRepository;
    @Autowired private MorphJpaRepository morphRepository;
    @Autowired private LifeStageJpaRepository lifeStageRepository;
    @Autowired private FileJpaRepository fileRepository;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private User owner;
    private User other;
    private Species crested;
    private Species leopard;
    private List<Morph> crestedMorphs;

    @BeforeEach
    void setUp() {
        petRepository.deleteAll();
        fileRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.save(User.pending(
                OAuthProvider.GOOGLE, "pet-owner", "소유자", Instant.parse("2026-01-01T00:00:00Z")));
        other = userRepository.save(User.pending(
                OAuthProvider.GOOGLE, "pet-other", "다른 사용자", Instant.parse("2026-01-01T00:00:00Z")));
        crested = species("CRESTED");
        leopard = species("LEOPARD");
        crestedMorphs = morphRepository.findAllBySpecies_IdOrderByDisplayOrderAsc(crested.getId());
    }

    @Test
    void registersPetWithMultipleMorphsAndCustomMorph() {
        PetResult result = commandService.create(owner.getId(), command(
                List.of(morph("HARLEQUIN").getId(), morph("DALMATIAN").getId()), "직접 입력"));

        assertThat(result.id()).isNotNull();
        assertThat(result.name()).isEqualTo("콩이");
        assertThat(result.morphs()).extracting(PetResult.MorphItem::code)
                .containsExactly("HARLEQUIN", "DALMATIAN");
        assertThat(result.customMorph()).isEqualTo("직접 입력");
    }

    @Test
    void registersPetWhenAllOptionalInformationIsSkipped() {
        CreatePetCommand command = new CreatePetCommand(
                UUID.randomUUID(), crested.getId(), " 모찌 ", null, null, null,
                null, null, null, null, null);

        PetResult result = commandService.create(owner.getId(), command);

        assertThat(result.name()).isEqualTo("모찌");
        assertThat(result.morphs()).isEmpty();
        assertThat(result.hatchedAt()).isNull();
        assertThat(result.adoptedAt()).isNull();
        assertThat(result.lifeStageId()).isNull();
        assertThat(result.stageCertainty()).isEqualTo(Certainty.UNKNOWN);
        assertThat(result.sex()).isEqualTo(Sex.UNKNOWN);
        assertThat(result.sexCertainty()).isEqualTo(Certainty.UNKNOWN);
    }

    @Test
    void acceptsNormalByItself() {
        PetResult result = commandService.create(
                owner.getId(), command(List.of(morph("NORMAL").getId()), null));

        assertThat(result.morphs()).extracting(PetResult.MorphItem::code).containsExactly("NORMAL");
    }

    @Test
    void rejectsInvalidMorphSelections() {
        assertError(
                command(List.of(morph("NORMAL").getId(), morph("FLAME").getId()), null),
                PetErrorCode.MORPH_COMBINATION_INVALID
        );
        assertError(
                command(List.of(morph("NORMAL").getId()), "기타"),
                PetErrorCode.MORPH_COMBINATION_INVALID
        );
        assertError(
                command(List.of(morph("FLAME").getId(), morph("FLAME").getId()), null),
                PetErrorCode.MORPH_DUPLICATED
        );
        Long leopardMorphId = morphRepository.findAllBySpecies_IdOrderByDisplayOrderAsc(leopard.getId()).get(0).getId();
        assertError(command(List.of(leopardMorphId), null), PetErrorCode.MORPH_INVALID);
        assertError(command(List.of(), "가".repeat(41)), PetErrorCode.CUSTOM_MORPH_INVALID);
    }

    @Test
    void returnsExistingPetForDuplicateClientRequestId() {
        UUID requestId = UUID.randomUUID();
        CreatePetCommand command = command(requestId, List.of(), null);

        PetResult first = commandService.create(owner.getId(), command);
        PetResult replay = commandService.create(owner.getId(), command);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(petRepository.count()).isEqualTo(1);
    }

    @Test
    void keepsOriginalResultWhenSameClientRequestIdIsReusedWithDifferentPayload() {
        UUID requestId = UUID.randomUUID();
        CreatePetCommand original = command(requestId, List.of(), null);
        CreatePetCommand changed = new CreatePetCommand(
                requestId, leopard.getId(), "다른 이름", List.of(), null,
                null, null, null, Certainty.UNKNOWN,
                Sex.UNKNOWN, Certainty.UNKNOWN, null
        );

        PetResult first = commandService.create(owner.getId(), original);
        PetResult replay = commandService.create(owner.getId(), changed);

        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(replay.name()).isEqualTo(first.name());
        assertThat(replay.speciesId()).isEqualTo(first.speciesId());
        assertThat(petRepository.count()).isEqualTo(1);
    }

    @Test
    void persistsNewPetWithNullDeletedAtAndListsItImmediatelyForSameOwner() {
        PetResult created = commandService.create(owner.getId(), command(List.of(), null));

        assertThat(created.id()).isNotNull();
        assertThat(petRepository.findById(created.id())).hasValueSatisfying(saved -> {
            assertThat(saved.getUserId()).isEqualTo(owner.getId());
            assertThat(saved.getDeletedAt()).isNull();
        });
        assertThat(queryService.getAll(owner.getId())).extracting(PetListResult::id)
                .containsExactly(created.id());
        assertThat(queryService.getAll(other.getId())).isEmpty();
    }

    @Test
    void reusingDeletedPetsClientRequestIdCreatesDistinctActivePet() {
        UUID requestId = UUID.randomUUID();
        CreatePetCommand create = command(requestId, List.of(), null);
        PetResult deleted = commandService.create(owner.getId(), create);
        PetResult remaining = commandService.create(owner.getId(), command(List.of(), null));

        commandService.delete(owner.getId(), deleted.id());

        assertThat(petRepository.findById(deleted.id()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(queryService.getAll(owner.getId())).extracting(PetListResult::id)
                .containsExactly(remaining.id());
        assertThatThrownBy(() -> queryService.get(owner.getId(), deleted.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));
        assertThatThrownBy(() -> commandService.update(owner.getId(), deleted.id(), emptyUpdate()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));
        assertThatThrownBy(() -> commandService.delete(owner.getId(), deleted.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));
        PetResult replacement = commandService.create(owner.getId(), create);
        PetResult replay = commandService.create(owner.getId(), create);

        assertThat(replacement.id()).isNotEqualTo(deleted.id());
        assertThat(replay.id()).isEqualTo(replacement.id());
        assertThat(petRepository.findById(deleted.id()).orElseThrow().getDeletedAt()).isNotNull();
        assertThat(petRepository.findById(replacement.id()).orElseThrow().getDeletedAt()).isNull();
        assertThat(queryService.getAll(owner.getId())).extracting(PetListResult::id)
                .containsExactly(replacement.id(), remaining.id());
        assertThat(petRepository.countByUserIdAndStatus(owner.getId(),
                com.pabom.backend.pet.domain.type.PetStatus.ACTIVE)).isEqualTo(2);
    }

    @Test
    void listsAndGetsOnlyOwnedPets() {
        PetResult olderOwned = commandService.create(owner.getId(), command(List.of(), null));
        PetResult newerOwned = commandService.create(owner.getId(), command(List.of(), null));
        commandService.create(other.getId(), command(List.of(), null));

        assertThat(queryService.getAll(owner.getId())).extracting(PetListResult::id)
                .containsExactly(newerOwned.id(), olderOwned.id());
        assertThat(queryService.get(owner.getId(), olderOwned.id()).id()).isEqualTo(olderOwned.id());
        assertThatThrownBy(() -> queryService.get(other.getId(), olderOwned.id()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));
    }

    @Test
    void listsScreenFieldsWithDisplayNamesAndNullableValues() {
        Long adultId = adultId();
        PetResult withStage = commandService.create(owner.getId(), withStage(
                command(List.of(morph("HARLEQUIN").getId()), "기타 모프"),
                adultId,
                Certainty.CONFIRMED
        ));
        PetResult withoutStage = commandService.create(owner.getId(), command(List.of(), null));

        List<PetListResult> results = queryService.getAll(owner.getId());

        PetListResult first = results.stream().filter(item -> item.id().equals(withStage.id())).findFirst().orElseThrow();
        assertThat(first.speciesName()).isEqualTo(crested.getCommonName());
        assertThat(first.lifeStageName()).isEqualTo("성체");
        assertThat(first.morphs()).extracting(PetResult.MorphItem::code).containsExactly("HARLEQUIN");
        assertThat(first.customMorph()).isEqualTo("기타 모프");
        assertThat(first.photoFileId()).isNull();

        PetListResult second = results.stream().filter(item -> item.id().equals(withoutStage.id())).findFirst().orElseThrow();
        assertThat(second.lifeStageId()).isNull();
        assertThat(second.lifeStageName()).isNull();
        assertThat(second.morphs()).isEmpty();
    }

    @Test
    void listQueryCountDoesNotGrowWithNumberOfPets() {
        commandService.create(owner.getId(), withStage(command(List.of(), null), adultId(), Certainty.CONFIRMED));
        commandService.create(owner.getId(), withStage(command(List.of(), null), adultId(), Certainty.CONFIRMED));
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        sessionFactory.getStatistics().clear();

        queryService.getAll(owner.getId());

        assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isEqualTo(3);
    }

    @Test
    void createsGetsAndPatchesAdoptedAtWithAbsentAndExplicitNullSemantics() {
        LocalDate original = LocalDate.of(2026, 4, 10);
        PetResult created = commandService.create(owner.getId(), withAdoptedAt(command(List.of(), null), original));
        assertThat(created.adoptedAt()).isEqualTo(original);
        assertThat(queryService.get(owner.getId(), created.id()).adoptedAt()).isEqualTo(original);

        PetResult omitted = commandService.update(owner.getId(), created.id(), emptyUpdate());
        assertThat(omitted.adoptedAt()).isEqualTo(original);

        UpdatePetCommand changed = new UpdatePetCommand(
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                PatchValue.of(LocalDate.of(2026, 5, 1)), PatchValue.absent(), PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent()
        );
        assertThat(commandService.update(owner.getId(), created.id(), changed).adoptedAt())
                .isEqualTo(LocalDate.of(2026, 5, 1));

        UpdatePetCommand removed = new UpdatePetCommand(
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                PatchValue.of(null), PatchValue.absent(), PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent()
        );
        assertThat(commandService.update(owner.getId(), created.id(), removed).adoptedAt()).isNull();
    }

    @Test
    void rejectsFutureAdoptedAtOnCreateAndPatch() {
        LocalDate future = LocalDate.now().plusDays(1);
        assertError(withAdoptedAt(command(List.of(), null), future), PetErrorCode.ADOPTED_AT_INVALID);
        PetResult created = commandService.create(owner.getId(), command(List.of(), null));
        UpdatePetCommand update = new UpdatePetCommand(
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                PatchValue.of(future), PatchValue.absent(), PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent()
        );
        assertUpdateError(created.id(), update, PetErrorCode.ADOPTED_AT_INVALID);
    }

    @Test
    void patchesOnlyPresentFieldsAndSupportsExplicitNullAndEmptyMorphs() {
        PetResult created = commandService.create(owner.getId(), command(
                List.of(morph("FLAME").getId()), "기타"));
        Long adultId = lifeStageRepository.findAll().stream()
                .filter(stage -> stage.getCode().equals("ADULT"))
                .findFirst().orElseThrow().getId();

        UpdatePetCommand update = new UpdatePetCommand(
                PatchValue.of(" 새 이름 "),
                PatchValue.of(List.of()),
                PatchValue.of(null),
                PatchValue.of(null),
                PatchValue.of(adultId),
                PatchValue.of(Certainty.ESTIMATED),
                PatchValue.of(Sex.FEMALE),
                PatchValue.of(Certainty.ESTIMATED),
                PatchValue.of(null)
        );
        PetResult result = commandService.update(owner.getId(), created.id(), update);

        assertThat(result.name()).isEqualTo("새 이름");
        assertThat(result.morphs()).isEmpty();
        assertThat(result.customMorph()).isNull();
        assertThat(result.hatchedAt()).isNull();
        assertThat(result.lifeStageId()).isEqualTo(adultId);
        assertThat(result.sex()).isEqualTo(Sex.FEMALE);
        assertThat(result.sexCertainty()).isEqualTo(Certainty.ESTIMATED);
        assertThat(result.photoFileId()).isNull();
    }

    @Test
    void rejectsOtherUsersUpdateAndFutureHatchDate() {
        PetResult created = commandService.create(owner.getId(), command(List.of(), null));

        assertThatThrownBy(() -> commandService.update(other.getId(), created.id(), emptyUpdate()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(PetErrorCode.PET_NOT_FOUND));
        CreatePetCommand future = new CreatePetCommand(
                UUID.randomUUID(), crested.getId(), "미래", List.of(), null,
                LocalDate.now().plusDays(1), null, Certainty.UNKNOWN,
                Sex.UNKNOWN, Certainty.UNKNOWN, null);
        assertError(future, PetErrorCode.HATCHED_AT_INVALID);
    }

    @Test
    void validatesPhotoOwnershipAndPurpose() {
        File photo = fileRepository.save(File.temporary(
                owner.getId(), FilePurpose.PET_PHOTO, "image/png", 3, 1, 1,
                "pets/owner-photo.png", Instant.parse("2026-10-18T00:00:00Z")
        ));
        File otherPhoto = fileRepository.save(File.temporary(
                other.getId(), FilePurpose.PET_PHOTO, "image/png", 3, 1, 1,
                "pets/other-photo.png", Instant.parse("2026-10-18T00:00:00Z")
        ));
        File wrongPurpose = fileRepository.save(File.temporary(
                owner.getId(), FilePurpose.RECORD_PHOTO, "image/png", 3, 1, 1,
                "records/wrong-purpose.png", Instant.parse("2026-10-18T00:00:00Z")
        ));

        CreatePetCommand valid = withPhoto(command(List.of(), null), photo.getId());
        assertThat(commandService.create(owner.getId(), valid).photoFileId()).isEqualTo(photo.getId());
        assertError(withPhoto(command(List.of(), null), otherPhoto.getId()), PetErrorCode.PET_PHOTO_INVALID);
        assertError(withPhoto(command(List.of(), null), wrongPurpose.getId()), PetErrorCode.PET_PHOTO_INVALID);
    }

    @Test
    void validatesNormalCombinationAgainstFinalPatchState() {
        PetResult created = commandService.create(owner.getId(), command(List.of(), "기타"));
        UpdatePetCommand update = new UpdatePetCommand(
                PatchValue.absent(),
                PatchValue.of(List.of(morph("NORMAL").getId())),
                PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent()
        );

        assertThatThrownBy(() -> commandService.update(owner.getId(), created.id(), update))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(PetErrorCode.MORPH_COMBINATION_INVALID));
    }

    @Test
    void validatesLifeStageCombinationsOnCreate() {
        Long adultId = adultId();

        assertError(withStage(command(List.of(), null), null, Certainty.CONFIRMED), PetErrorCode.STAGE_REQUIRED);
        assertError(withStage(command(List.of(), null), null, Certainty.ESTIMATED), PetErrorCode.STAGE_REQUIRED);
        assertThat(commandService.create(owner.getId(),
                withStage(command(List.of(), null), null, Certainty.UNKNOWN)).lifeStageId()).isNull();
        assertError(withStage(command(List.of(), null), adultId, Certainty.UNKNOWN),
                SpeciesErrorCode.LIFE_STAGE_INVALID);
        assertThat(commandService.create(owner.getId(),
                withStage(command(List.of(), null), adultId, Certainty.CONFIRMED)).lifeStageId())
                .isEqualTo(adultId);
        assertThat(commandService.create(owner.getId(),
                withStage(command(List.of(), null), adultId, Certainty.ESTIMATED)).lifeStageId())
                .isEqualTo(adultId);
        assertError(withStage(command(List.of(), null), 999L, Certainty.CONFIRMED),
                SpeciesErrorCode.LIFE_STAGE_INVALID);
    }

    @Test
    void validatesMergedLifeStageStateOnPatch() {
        Long adultId = adultId();
        PetResult created = commandService.create(owner.getId(),
                withStage(command(List.of(), null), adultId, Certainty.CONFIRMED));

        assertUpdateError(created.id(), stagePatch(PatchValue.absent(), PatchValue.of(Certainty.UNKNOWN)),
                SpeciesErrorCode.LIFE_STAGE_INVALID);
        assertUpdateError(created.id(), stagePatch(PatchValue.of(null), PatchValue.absent()),
                PetErrorCode.STAGE_REQUIRED);

        PetResult unknown = commandService.update(owner.getId(), created.id(),
                stagePatch(PatchValue.of(null), PatchValue.of(Certainty.UNKNOWN)));
        assertThat(unknown.lifeStageId()).isNull();
        assertThat(unknown.stageCertainty()).isEqualTo(Certainty.UNKNOWN);

        PetResult estimated = commandService.update(owner.getId(), created.id(),
                stagePatch(PatchValue.of(adultId), PatchValue.of(Certainty.ESTIMATED)));
        assertThat(estimated.lifeStageId()).isEqualTo(adultId);
        assertThat(estimated.stageCertainty()).isEqualTo(Certainty.ESTIMATED);
    }

    private CreatePetCommand command(List<Long> morphIds, String customMorph) {
        return command(UUID.randomUUID(), morphIds, customMorph);
    }

    private CreatePetCommand command(UUID requestId, List<Long> morphIds, String customMorph) {
        return new CreatePetCommand(
                requestId, crested.getId(), "콩이", morphIds, customMorph,
                LocalDate.of(2026, 3, 2), null, Certainty.UNKNOWN,
                Sex.FEMALE, Certainty.ESTIMATED, null
        );
    }

    private UpdatePetCommand emptyUpdate() {
        return new UpdatePetCommand(
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent()
        );
    }

    private CreatePetCommand withPhoto(CreatePetCommand source, Long photoFileId) {
        return new CreatePetCommand(
                source.clientRequestId(), source.speciesId(), source.name(), source.morphIds(),
                source.customMorph(), source.hatchedAt(), source.lifeStageId(), source.stageCertainty(),
                source.sex(), source.sexCertainty(), photoFileId
        );
    }

    private CreatePetCommand withAdoptedAt(CreatePetCommand source, LocalDate adoptedAt) {
        return new CreatePetCommand(
                source.clientRequestId(), source.speciesId(), source.name(), source.morphIds(),
                source.customMorph(), source.hatchedAt(), adoptedAt, source.lifeStageId(),
                source.stageCertainty(), source.sex(), source.sexCertainty(), source.photoFileId()
        );
    }

    private CreatePetCommand withStage(
            CreatePetCommand source,
            Long lifeStageId,
            Certainty stageCertainty
    ) {
        return new CreatePetCommand(
                UUID.randomUUID(), source.speciesId(), source.name(), source.morphIds(),
                source.customMorph(), source.hatchedAt(), lifeStageId, stageCertainty,
                source.sex(), source.sexCertainty(), source.photoFileId()
        );
    }

    private UpdatePetCommand stagePatch(
            PatchValue<Long> lifeStageId,
            PatchValue<Certainty> stageCertainty
    ) {
        return new UpdatePetCommand(
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent(), PatchValue.absent(),
                lifeStageId, stageCertainty,
                PatchValue.absent(), PatchValue.absent(), PatchValue.absent()
        );
    }

    private void assertUpdateError(Long petId, UpdatePetCommand command, Object errorCode) {
        assertThatThrownBy(() -> commandService.update(owner.getId(), petId, command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(errorCode));
    }

    private Long adultId() {
        return lifeStageRepository.findAll().stream()
                .filter(stage -> stage.getCode().equals("ADULT"))
                .findFirst().orElseThrow().getId();
    }

    private Species species(String code) {
        return speciesRepository.findAll().stream()
                .filter(item -> item.getCode().equals(code))
                .findFirst().orElseThrow();
    }

    private Morph morph(String code) {
        return crestedMorphs.stream().filter(item -> item.getCode().equals(code)).findFirst().orElseThrow();
    }

    private void assertError(CreatePetCommand command, Object errorCode) {
        assertThatThrownBy(() -> commandService.create(owner.getId(), command))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(errorCode));
    }
}
