package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.NotificationSetting;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationSettingJpaRepository extends JpaRepository<NotificationSetting, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NotificationSetting s where s.userId = :userId")
    Optional<NotificationSetting> findByUserIdForUpdate(@Param("userId") Long userId);

    List<NotificationSetting> findAllByUserIdIn(Collection<Long> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NotificationSetting s where s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
