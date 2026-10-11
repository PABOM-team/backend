package com.pabom.backend.event.infrastructure.persistence;

import com.pabom.backend.event.domain.entity.LoginEvent;
import com.pabom.backend.event.domain.repository.LoginEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LoginEventPersistenceAdapter implements LoginEventRepository {

    private final LoginEventJpaRepository loginEventJpaRepository;

    @Override
    public LoginEvent save(LoginEvent loginEvent) {
        return loginEventJpaRepository.save(loginEvent);
    }
}
