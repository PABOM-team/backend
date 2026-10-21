package com.pabom.backend.pet.domain.repository;

import com.pabom.backend.pet.domain.entity.Pet;
import java.util.List;
import java.util.Optional;
import com.pabom.backend.pet.domain.type.PetStatus;

public interface PetRepository {

    Pet save(Pet pet);

    Optional<Pet> findByUserIdAndClientRequestId(Long userId, String clientRequestId);

    Optional<Pet> findByUserIdAndClientRequestIdForUpdate(Long userId, String clientRequestId);

    List<Pet> findAllByUserId(Long userId);

    List<Pet> findAllIncludingDeletedByUserId(Long userId);

    Optional<Pet> findByIdAndUserId(Long id, Long userId);

    Optional<Pet> findActiveByIdAndUserIdForUpdate(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, PetStatus status);

    void deleteAllByUserId(Long userId);
}
