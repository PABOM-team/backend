package com.pabom.backend.user.domain.repository;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.user.domain.aggregate.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findByProviderAndProviderId(OAuthProvider provider, String providerId);

    Optional<User> findById(Long id);

    User save(User user);
}
