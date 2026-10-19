package com.pabom.backend.user.infrastructure.persistence;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.user.domain.aggregate.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.provider = :provider and user.providerId = :providerId")
    Optional<User> findByProviderAndProviderIdForUpdate(
            @Param("provider") OAuthProvider provider,
            @Param("providerId") String providerId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
