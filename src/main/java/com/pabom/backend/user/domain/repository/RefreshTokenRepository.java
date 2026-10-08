package com.pabom.backend.user.domain.repository;

import com.pabom.backend.user.domain.entity.RefreshToken;

public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken refreshToken);
}
