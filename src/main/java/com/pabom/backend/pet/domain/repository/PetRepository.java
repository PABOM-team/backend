package com.pabom.backend.pet.domain.repository;

import com.pabom.backend.pet.domain.entity.Pet;
import java.util.List;
import java.util.Optional;

public interface PetRepository {

    Pet save(Pet pet);

    Optional<Pet> findByUserIdAndClientRequestId(Long userId, String clientRequestId);

    List<Pet> findAllByUserId(Long userId);

    Optional<Pet> findByIdAndUserId(Long id, Long userId);
}
