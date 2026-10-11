package com.pabom.backend.event.domain.repository;

import com.pabom.backend.event.domain.entity.LoginEvent;

public interface LoginEventRepository {

    LoginEvent save(LoginEvent loginEvent);
}
