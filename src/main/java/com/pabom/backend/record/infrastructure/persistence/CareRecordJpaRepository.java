package com.pabom.backend.record.infrastructure.persistence;

import com.pabom.backend.record.domain.entity.CareRecord;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CareRecordJpaRepository extends JpaRepository<CareRecord, Long>, JpaSpecificationExecutor<CareRecord> {
    Optional<CareRecord> findByUserIdAndClientRequestId(Long userId, String clientRequestId);
    Optional<CareRecord> findByIdAndUserId(Long id, Long userId);

    Optional<CareRecord> findFirstByUserIdAndPetIdAndCareItemIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(
            Long userId, Long petId, Long careItemId);

    Optional<CareRecord> findFirstByUserIdAndRoutineIdAndDeletedAtIsNullOrderByOccurredAtDescIdDesc(
            Long userId, Long routineId);

    List<CareRecord> findAllByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CareRecord r where r.userId=:userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
