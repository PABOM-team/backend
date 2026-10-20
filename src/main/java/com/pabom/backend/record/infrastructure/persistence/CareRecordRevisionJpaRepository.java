package com.pabom.backend.record.infrastructure.persistence;

import com.pabom.backend.record.domain.entity.CareRecordRevision;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CareRecordRevisionJpaRepository extends JpaRepository<CareRecordRevision, Long> {
    List<CareRecordRevision> findAllByCareRecordIdOrderByIdAsc(Long careRecordId);
    long countByCareRecordIdAndChangeType(Long careRecordId,
            com.pabom.backend.record.domain.type.RevisionChangeType changeType);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from CareRecordRevision r where r.careRecordId in "
            + "(select c.id from CareRecord c where c.userId=:userId)")
    void deleteAllByUserId(@Param("userId") Long userId);
}
