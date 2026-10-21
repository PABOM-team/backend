package com.pabom.backend.pet.infrastructure.persistence;

import com.pabom.backend.pet.domain.entity.Pet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.pabom.backend.pet.domain.type.PetStatus;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;

public interface PetJpaRepository extends JpaRepository<Pet, Long> {

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.userId = :userId and p.clientRequestId = :clientRequestId")
    Optional<Pet> findOwnedByClientRequestId(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.userId = :userId and p.clientRequestId = :clientRequestId")
    Optional<Pet> findOwnedByClientRequestIdForUpdate(
            @Param("userId") Long userId,
            @Param("clientRequestId") String clientRequestId
    );

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.userId = :userId and p.deletedAt is null "
            + "order by p.createdAt desc, p.id desc")
    List<Pet> findAllOwned(@Param("userId") Long userId);

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.userId = :userId order by p.createdAt desc, p.id desc")
    List<Pet> findAllOwnedIncludingDeleted(@Param("userId") Long userId);

    @Query("select distinct p from Pet p left join fetch p.morphs "
            + "where p.id = :id and p.userId = :userId and p.deletedAt is null")
    Optional<Pet> findOwnedById(@Param("id") Long id, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id and p.userId = :userId and p.deletedAt is null")
    Optional<Pet> findActiveOwnedByIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    @Query("select p.id from Pet p where p.userId = :userId and p.deletedAt is null")
    List<Long> findActiveIdsByUserId(@Param("userId") Long userId);

    @Query("select p.id from Pet p where p.deletedAt <= :cutoff order by p.deletedAt, p.id")
    List<Long> findCleanupCandidateIds(@Param("cutoff") java.time.Instant cutoff, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pet p where p.id = :id and p.deletedAt is not null")
    Optional<Pet> findDeletedByIdForUpdate(@Param("id") Long id);

    long countByPhotoFileIdAndIdNot(Long photoFileId, Long id);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from pet_morphs where pet_id = :petId", nativeQuery = true)
    void deleteMorphLinksByPetId(@Param("petId") Long petId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Pet pet where pet.id = :petId")
    void deleteByPetId(@Param("petId") Long petId);

    @Query("select count(p) from Pet p where p.userId=:userId and p.status=:status and p.deletedAt is null")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PetStatus status);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from pet_morphs where pet_id in "
            + "(select id from pets where user_id = :userId)", nativeQuery = true)
    void deleteMorphLinksByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Pet pet where pet.userId = :userId")
    void deleteAllOwnedByUserId(@Param("userId") Long userId);
}
