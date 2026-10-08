package com.pabom.backend.event.infrastructure.persistence;

import com.pabom.backend.event.domain.entity.LoginEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginEventJpaRepository extends JpaRepository<LoginEvent, Long> {
}
