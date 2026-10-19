package com.pabom.backend.pet.infrastructure.persistence;

import com.pabom.backend.pet.domain.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.pabom.backend.pet.domain.type.PetStatus;
import org.springframework.data.repository.query.Param;

public interface PetJpaRepository extends JpaRepository<Pet, Long> {

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.userId = :userId and p.clientRequestId = :clientRequestId")
    Optional<Pet> findOwnedByClientRequestId(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.userId = :userId order by p.createdAt desc, p.id desc")
    List<Pet> findAllOwned(@Param("userId") Long userId);

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.id = :id and p.userId = :userId")
    Optional<Pet> findOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    long countByUserIdAndStatus(Long userId, PetStatus status);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from pet_morphs where pet_id in "
            + "(select id from pets where user_id = :userId)", nativeQuery = true)
    void deleteMorphLinksByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Pet pet where pet.userId = :userId")
    void deleteAllOwnedByUserId(@Param("userId") Long userId);
}
