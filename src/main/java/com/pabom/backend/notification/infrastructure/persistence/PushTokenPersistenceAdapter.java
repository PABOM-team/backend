package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PushTokenPersistenceAdapter implements PushTokenRepository {

    private final PushTokenJpaRepository repository;

    @Override
    public void upsert(Long userId, String token, PushPlatform platform, AppVariant appVariant, Instant now) {
        repository.upsert(userId, token, platform.name(), appVariant.name(), now);
    }

    @Override
    public Optional<PushToken> findByToken(String token) {
        return repository.findByToken(token);
    }

    @Override
    public List<PushToken> findAllByUserId(Long userId) {
        return repository.findAllByUserIdOrderByIdAsc(userId);
    }

    @Override
    public List<PushToken> findAllByUserIdIn(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return repository.findAllByUserIdInOrderByIdAsc(userIds);
    }

    @Override
    public List<Long> findDistinctUserIds() {
        return repository.findDistinctUserIds();
    }

    @Override
    public void deleteByUserIdAndToken(Long userId, String token) {
        repository.deleteByUserIdAndToken(userId, token);
    }

    @Override
    public void deleteByToken(String token) {
        repository.deleteByToken(token);
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
