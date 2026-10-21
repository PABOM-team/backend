package com.pabom.backend.pet.application.service;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.application.command.CreatePetCommand;
import com.pabom.backend.pet.application.command.UpdatePetCommand;
import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.pet.domain.type.Sex;
import com.pabom.backend.species.domain.entity.Morph;
import com.pabom.backend.record.infrastructure.persistence.CareRecordJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetCommandService {

    private final PetRepository petRepository;
    private final PetValidator validator;
    private final Clock clock;
    private final CareRecordJpaRepository careRecordRepository;

    @Transactional
    public PetResult create(Long userId, CreatePetCommand command) {
        if (command.clientRequestId() == null) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }
        String requestId = command.clientRequestId().toString();
        Pet existing = petRepository.findByUserIdAndClientRequestIdForUpdate(userId, requestId).orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() != null) {
                throw new BusinessException(PetErrorCode.PET_NOT_FOUND);
            }
            return PetResult.from(existing);
        }

        validator.validateSpecies(command.speciesId());
        String name = validator.normalizeName(command.name());
        String customMorph = validator.normalizeCustomMorph(command.customMorph());
        List<Morph> morphs = validator.resolveMorphs(command.speciesId(), command.morphIds());
        validator.validateMorphCombination(morphs, customMorph);
        validator.validateHatchedAt(command.hatchedAt());
        validator.validateAdoptedAt(command.adoptedAt());
        validator.validatePhoto(command.photoFileId(), userId);

        Certainty stageCertainty = defaultCertainty(command.stageCertainty());
        validator.validateLifeStage(command.lifeStageId(), stageCertainty);
        Sex sex = command.sex() == null ? Sex.UNKNOWN : command.sex();
        Certainty sexCertainty = defaultCertainty(command.sexCertainty());
        Instant now = clock.instant();
        Pet pet = Pet.create(
                userId,
                requestId,
                command.speciesId(),
                name,
                morphs,
                customMorph,
                command.hatchedAt(),
                command.adoptedAt(),
                command.lifeStageId(),
                stageCertainty,
                sex,
                sexCertainty,
                command.photoFileId(),
                now
        );
        return PetResult.from(petRepository.save(pet));
    }

    @Transactional
    public void delete(Long userId, Long petId) {
        Pet pet = petRepository.findActiveByIdAndUserIdForUpdate(petId, userId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
        Instant now = clock.instant();
        pet.delete(now);
        careRecordRepository.softDeleteActiveByPetId(petId, now);
    }

    @Transactional
    public PetResult update(Long userId, Long petId, UpdatePetCommand command) {
        Pet pet = petRepository.findActiveByIdAndUserIdForUpdate(petId, userId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));

        List<Morph> finalMorphs = pet.getMorphs();
        if (command.morphIds().present()) {
            if (command.morphIds().value() == null) {
                throw new BusinessException(PetErrorCode.MORPH_INVALID);
            }
            finalMorphs = validator.resolveMorphs(pet.getSpeciesId(), command.morphIds().value());
        }
        String finalCustomMorph = pet.getCustomMorph();
        if (command.customMorph().present()) {
            finalCustomMorph = validator.normalizeCustomMorph(command.customMorph().value());
        }
        validator.validateMorphCombination(finalMorphs, finalCustomMorph);

        Long finalLifeStageId = command.lifeStageId().present()
                ? command.lifeStageId().value()
                : pet.getLifeStageId();
        Certainty finalStageCertainty = command.stageCertainty().present()
                ? requireValue(command.stageCertainty().value())
                : pet.getStageCertainty();
        validator.validateLifeStage(finalLifeStageId, finalStageCertainty);

        Instant now = clock.instant();
        if (command.name().present()) {
            pet.updateName(validator.normalizeName(command.name().value()), now);
        }
        if (command.morphIds().present()) {
            pet.updateMorphs(finalMorphs, now);
        }
        if (command.customMorph().present()) {
            pet.updateCustomMorph(finalCustomMorph, now);
        }
        if (command.hatchedAt().present()) {
            validator.validateHatchedAt(command.hatchedAt().value());
            pet.updateHatchedAt(command.hatchedAt().value(), now);
        }
        if (command.adoptedAt().present()) {
            validator.validateAdoptedAt(command.adoptedAt().value());
            pet.updateAdoptedAt(command.adoptedAt().value(), now);
        }
        if (command.lifeStageId().present()) {
            pet.updateLifeStage(command.lifeStageId().value(), now);
        }
        if (command.stageCertainty().present()) {
            pet.updateStageCertainty(finalStageCertainty, now);
        }
        if (command.sex().present()) {
            pet.updateSex(requireValue(command.sex().value()), now);
        }
        if (command.sexCertainty().present()) {
            pet.updateSexCertainty(requireValue(command.sexCertainty().value()), now);
        }
        if (command.photoFileId().present()) {
            validator.validatePhoto(command.photoFileId().value(), userId);
            pet.updatePhotoFileId(command.photoFileId().value(), now);
        }
        return PetResult.from(petRepository.save(pet));
    }

    private Certainty defaultCertainty(Certainty certainty) {
        return certainty == null ? Certainty.UNKNOWN : certainty;
    }

    private <T> T requireValue(T value) {
        if (value == null) {
            throw new BusinessException(AuthErrorCode.VALIDATION_FAILED);
        }
        return value;
    }
}
