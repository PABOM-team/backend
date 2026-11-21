package com.pabom.backend.notification.infrastructure.persistence;

import com.pabom.backend.notification.domain.entity.PushToken;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenJpaRepository extends JpaRepository<PushToken, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            INSERT INTO push_tokens (user_id, token, platform, app_variant, created_at, last_seen_at)
            VALUES (:userId, :token, :platform, :appVariant, :now, :now)
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id),
                platform = VALUES(platform),
                app_variant = VALUES(app_variant),
                last_seen_at = VALUES(last_seen_at)
            """, nativeQuery = true)
    void upsert(
            @Param("userId") Long userId,
            @Param("token") String token,
            @Param("platform") String platform,
            @Param("appVariant") String appVariant,
            @Param("now") Instant now
    );

    Optional<PushToken> findByToken(String token);

    List<PushToken> findAllByUserIdOrderByIdAsc(Long userId);

    List<PushToken> findAllByUserIdInOrderByIdAsc(Collection<Long> userIds);

    @Query("select distinct t.userId from PushToken t order by t.userId")
    List<Long> findDistinctUserIds();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PushToken t where t.userId = :userId and t.token = :token")
    void deleteByUserIdAndToken(@Param("userId") Long userId, @Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PushToken t where t.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PushToken t where t.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
