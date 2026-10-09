package com.pabom.backend.user.infrastructure.persistence;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByProviderAndProviderId(
            OAuthProvider provider,
            String providerId
    ) {
        return userJpaRepository.findByProviderAndProviderId(provider, providerId);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }
}
