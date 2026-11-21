package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.NotificationSetting;
import com.pabom.backend.notification.domain.repository.NotificationSettingRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationSettingPersistenceAdapter implements NotificationSettingRepository {

    private final NotificationSettingJpaRepository repository;

    @Override
    public Optional<NotificationSetting> findByUserId(Long userId) {
        return repository.findById(userId);
    }

    @Override
    public Optional<NotificationSetting> findByUserIdForUpdate(Long userId) {
        return repository.findByUserIdForUpdate(userId);
    }

    @Override
    public List<NotificationSetting> findAllByUserIdIn(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUserIdIn(userIds);
    }

    @Override
    public NotificationSetting save(NotificationSetting setting) {
        return repository.save(setting);
    }

    @Override
    public void deleteByUserId(Long userId) {
        repository.deleteByUserId(userId);
    }
}
