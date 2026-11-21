package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.PushTicket;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTicketJpaRepository extends JpaRepository<PushTicket, Long> {

    @Query("select t from PushTicket t where t.createdAt <= :cutoff order by t.createdAt, t.id")
    List<PushTicket> findCreatedAtOrBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PushTicket t where t.id in :ids")
    void deleteAllByIdIn(@Param("ids") Collection<Long> ids);
}
