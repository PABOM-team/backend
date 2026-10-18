package com.pabom.backend.pet.application.service;

import com.pabom.backend.file.domain.entity.File;
import com.pabom.backend.file.domain.repository.FileRepository;
import com.pabom.backend.file.domain.type.FilePurpose;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.type.Certainty;
import com.pabom.backend.species.domain.entity.Morph;
import com.pabom.backend.species.domain.error.SpeciesErrorCode;
import com.pabom.backend.species.domain.repository.LifeStageRepository;
import com.pabom.backend.species.domain.repository.MorphRepository;
import com.pabom.backend.species.domain.repository.SpeciesRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PetValidator {

    private final SpeciesRepository speciesRepository;
    private final MorphRepository morphRepository;
    private final LifeStageRepository lifeStageRepository;
    private final FileRepository fileRepository;
    private final Clock clock;

    public void validateSpecies(Long speciesId) {
        if (speciesId == null || speciesRepository.findById(speciesId).isEmpty()) {
            throw new BusinessException(SpeciesErrorCode.SPECIES_NOT_FOUND);
        }
    }

    public String normalizeName(String name) {
        if (name == null) {
            throw new BusinessException(PetErrorCode.PET_NAME_INVALID);
        }
        String normalized = name.trim();
        if (normalized.isEmpty() || normalized.length() > 20) {
            throw new BusinessException(PetErrorCode.PET_NAME_INVALID);
        }
        return normalized;
    }

    public String normalizeCustomMorph(String customMorph) {
        if (customMorph == null) {
            return null;
        }
        String normalized = customMorph.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > 40) {
            throw new BusinessException(PetErrorCode.CUSTOM_MORPH_INVALID);
        }
        return normalized;
    }

    public List<Morph> resolveMorphs(Long speciesId, List<Long> morphIds) {
        List<Long> ids = morphIds == null ? List.of() : morphIds;
        if (ids.stream().anyMatch(id -> id == null)) {
            throw new BusinessException(PetErrorCode.MORPH_INVALID);
        }
        Set<Long> distinctIds = new HashSet<>(ids);
        if (distinctIds.size() != ids.size()) {
            throw new BusinessException(PetErrorCode.MORPH_DUPLICATED);
        }
        List<Morph> morphs = morphRepository.findAllById(ids);
        if (morphs.size() != ids.size()
                || morphs.stream().anyMatch(morph -> !speciesId.equals(morph.getSpeciesId()))) {
            throw new BusinessException(PetErrorCode.MORPH_INVALID);
        }
        return morphs;
    }

    public void validateMorphCombination(List<Morph> morphs, String customMorph) {
        boolean containsNormal = morphs.stream().anyMatch(Morph::isNormal);
        if (containsNormal && (morphs.size() > 1 || customMorph != null)) {
            throw new BusinessException(PetErrorCode.MORPH_COMBINATION_INVALID);
        }
    }

    public void validateHatchedAt(LocalDate hatchedAt) {
        if (hatchedAt != null && hatchedAt.isAfter(LocalDate.now(clock))) {
            throw new BusinessException(PetErrorCode.HATCHED_AT_INVALID);
        }
    }

    public void validateAdoptedAt(LocalDate adoptedAt) {
        if (adoptedAt != null && adoptedAt.isAfter(LocalDate.now(clock))) {
            throw new BusinessException(PetErrorCode.ADOPTED_AT_INVALID);
        }
    }

    public void validateLifeStage(Long lifeStageId, Certainty stageCertainty) {
        if (stageCertainty == Certainty.UNKNOWN && lifeStageId != null) {
            throw new BusinessException(SpeciesErrorCode.LIFE_STAGE_INVALID);
        }
        if ((stageCertainty == Certainty.CONFIRMED || stageCertainty == Certainty.ESTIMATED)
                && lifeStageId == null) {
            throw new BusinessException(PetErrorCode.STAGE_REQUIRED);
        }
        if (lifeStageId != null && !lifeStageRepository.existsById(lifeStageId)) {
            throw new BusinessException(SpeciesErrorCode.LIFE_STAGE_INVALID);
        }
    }

    public void validatePhoto(Long photoFileId, Long userId) {
        if (photoFileId == null) {
            return;
        }
        File file = fileRepository.findAccessibleByIdAndOwnerId(photoFileId, userId)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_PHOTO_INVALID));
        if (file.getPurpose() != FilePurpose.PET_PHOTO) {
            throw new BusinessException(PetErrorCode.PET_PHOTO_INVALID);
        }
    }
}
