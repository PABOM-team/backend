package com.pabom.backend.pet.application.service;

import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.pet.application.result.PetResult;
import com.pabom.backend.pet.application.result.PetListResult;
import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.error.PetErrorCode;
import com.pabom.backend.pet.domain.repository.PetRepository;
import com.pabom.backend.species.domain.entity.LifeStage;
import com.pabom.backend.species.domain.entity.Species;
import com.pabom.backend.species.domain.repository.LifeStageRepository;
import com.pabom.backend.species.domain.repository.SpeciesRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetQueryService {

    private final PetRepository petRepository;
    private final SpeciesRepository speciesRepository;
    private final LifeStageRepository lifeStageRepository;

    public List<PetListResult> getAll(Long userId) {
        List<Pet> pets = petRepository.findAllByUserId(userId);
        Map<Long, Species> speciesById = speciesRepository.findAllById(
                        pets.stream().map(Pet::getSpeciesId).distinct().toList())
                .stream().collect(Collectors.toMap(Species::getId, Function.identity()));
        Map<Long, LifeStage> lifeStageById = lifeStageRepository.findAllById(
                        pets.stream().map(Pet::getLifeStageId).filter(java.util.Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(LifeStage::getId, Function.identity()));

        return pets.stream()
                .map(pet -> PetListResult.from(
                        pet,
                        speciesById.get(pet.getSpeciesId()).getCommonName(),
                        pet.getLifeStageId() == null ? null : lifeStageById.get(pet.getLifeStageId()).getName()
                ))
                .toList();
    }

    public PetResult get(Long userId, Long petId) {
        return petRepository.findByIdAndUserId(petId, userId)
                .map(PetResult::from)
                .orElseThrow(() -> new BusinessException(PetErrorCode.PET_NOT_FOUND));
    }
}
