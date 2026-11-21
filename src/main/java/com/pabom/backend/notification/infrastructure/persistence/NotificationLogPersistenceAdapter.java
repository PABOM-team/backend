package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.NotificationLog;
import com.pabom.backend.notification.domain.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationLogPersistenceAdapter implements NotificationLogRepository {

    private final NotificationLogJpaRepository repository;

    @Override
    public void saveAndFlush(NotificationLog log) {
        repository.saveAndFlush(log);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
