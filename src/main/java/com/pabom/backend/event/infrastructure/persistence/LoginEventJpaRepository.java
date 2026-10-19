package com.pabom.backend.event.infrastructure.persistence;

import com.pabom.backend.event.domain.entity.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginEventJpaRepository extends JpaRepository<LoginEvent, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update LoginEvent event set event.userId = null where event.userId = :userId")
    void anonymizeByUserId(@Param("userId") Long userId);
}
