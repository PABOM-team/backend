package com.pabom.backend.user.infrastructure.persistence;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.user.domain.aggregate.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);
}
