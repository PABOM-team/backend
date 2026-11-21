package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogJpaRepository extends JpaRepository<NotificationLog, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NotificationLog l where l.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
