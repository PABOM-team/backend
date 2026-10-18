package com.pabom.backend.pet.infrastructure.persistence;

import com.pabom.backend.pet.domain.entity.Pet;
import com.pabom.backend.pet.domain.repository.PetRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PetPersistenceAdapter implements PetRepository {

    private final PetJpaRepository repository;

    @Override
    public Pet save(Pet pet) {
        return repository.saveAndFlush(pet);
    }

    @Override
    public Optional<Pet> findByUserIdAndClientRequestId(Long userId, String clientRequestId) {
        return repository.findOwnedByClientRequestId(userId, clientRequestId);
    }

    @Override
    public List<Pet> findAllByUserId(Long userId) {
        return repository.findAllOwned(userId);
    }

    @Override
    public Optional<Pet> findByIdAndUserId(Long id, Long userId) {
        return repository.findOwnedById(id, userId);
    }
}
