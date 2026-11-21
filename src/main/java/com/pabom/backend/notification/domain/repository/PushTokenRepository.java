package com.pabom.backend.notification.domain.repository;

import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushTokenRepository {

    /** 토큰이 없으면 만들고, 있으면 소유자·플랫폼·앱 종류·lastSeenAt을 갱신한다. */
    void upsert(Long userId, String token, PushPlatform platform, AppVariant appVariant, Instant now);

    Optional<PushToken> findByToken(String token);

    List<PushToken> findAllByUserId(Long userId);

    List<PushToken> findAllByUserIdIn(Collection<Long> userIds);

    List<Long> findDistinctUserIds();

    void deleteByUserIdAndToken(Long userId, String token);

    void deleteByToken(String token);

    void deleteAllByUserId(Long userId);
}
