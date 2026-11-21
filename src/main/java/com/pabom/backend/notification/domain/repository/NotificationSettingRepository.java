package com.pabom.backend.notification.domain.repository;

import com.pabom.backend.notification.domain.entity.NotificationSetting;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository {

    Optional<NotificationSetting> findByUserId(Long userId);

    Optional<NotificationSetting> findByUserIdForUpdate(Long userId);

    List<NotificationSetting> findAllByUserIdIn(Collection<Long> userIds);

    NotificationSetting save(NotificationSetting setting);

    void deleteByUserId(Long userId);
}
