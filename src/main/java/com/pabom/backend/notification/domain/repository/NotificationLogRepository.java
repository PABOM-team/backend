package com.pabom.backend.notification.domain.repository;

import com.pabom.backend.notification.domain.entity.NotificationLog;

public interface NotificationLogRepository {

    /** UNIQUE 위반이면 DataIntegrityViolationException을 던진다. */
    void saveAndFlush(NotificationLog log);

    void deleteAllByUserId(Long userId);
}
