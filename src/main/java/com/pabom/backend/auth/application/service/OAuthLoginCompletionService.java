package com.pabom.backend.auth.application.service;

import com.pabom.backend.auth.application.result.OAuthLoginCompletionResult;
import com.pabom.backend.auth.application.result.OAuthLoginResult;
import com.pabom.backend.auth.application.port.ServiceTokenPort;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedRefreshToken;
import com.pabom.backend.auth.application.port.ServiceTokenPort.IssuedToken;
import com.pabom.backend.event.domain.entity.LoginEvent;
import com.pabom.backend.event.domain.repository.LoginEventRepository;
import com.pabom.backend.user.application.service.TemporaryNicknameGenerator;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.domain.entity.RefreshToken;
import com.pabom.backend.user.domain.repository.RefreshTokenRepository;
import com.pabom.backend.user.domain.repository.UserRepository;
import com.pabom.backend.user.domain.type.UserStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OAuthLoginCompletionService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginEventRepository loginEventRepository;
    private final ServiceTokenPort tokenProvider;
    private final TemporaryNicknameGenerator nicknameGenerator;
    private final Clock clock;

    public OAuthLoginCompletionService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            LoginEventRepository loginEventRepository,
            ServiceTokenPort tokenProvider,
            TemporaryNicknameGenerator nicknameGenerator,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.loginEventRepository = loginEventRepository;
        this.tokenProvider = tokenProvider;
        this.nicknameGenerator = nicknameGenerator;
        this.clock = clock;
    }

    @Transactional
    public OAuthLoginCompletionResult complete(OAuthLoginResult socialLogin) {
        Instant loginAt = clock.instant();
        UserLookup lookup = findOrCreateUser(socialLogin, loginAt);
        User user = lookup.user();

        user.recordLogin(loginAt);
        userRepository.save(user);
        loginEventRepository.save(LoginEvent.succeeded(user.getId(), lookup.newUser(), loginAt));

        if (user.getStatus() == UserStatus.PENDING_TERMS) {
            IssuedToken signupToken = tokenProvider.issueSignupToken(user.getId(), loginAt);
            return OAuthLoginCompletionResult.signup(
                    signupToken.value(),
                    signupToken.expiresIn(),
                    lookup.newUser(),
                    user
            );
        }

        IssuedToken accessToken = tokenProvider.issueAccessToken(user.getId(), loginAt);
        IssuedRefreshToken refreshToken = tokenProvider.issueRefreshToken(user.getId(), loginAt);
        refreshTokenRepository.save(new RefreshToken(
                refreshToken.hash(),
                user.getId(),
                refreshToken.familyId(),
                refreshToken.issuedAt(),
                refreshToken.expiresAt()
        ));
        return OAuthLoginCompletionResult.access(
                accessToken.value(),
                accessToken.expiresIn(),
                refreshToken.value(),
                refreshToken.expiresIn(),
                user
        );
    }

    private UserLookup findOrCreateUser(OAuthLoginResult socialLogin, Instant loginAt) {
        return userRepository
                .findByProviderAndProviderId(socialLogin.provider(), socialLogin.providerId())
                .map(user -> new UserLookup(user, false))
                .orElseGet(() -> {
                    String nickname = StringUtils.hasText(socialLogin.nickname())
                            ? socialLogin.nickname()
                            : nicknameGenerator.generate();
                    User user = User.pending(
                            socialLogin.provider(),
                            socialLogin.providerId(),
                            nickname,
                            loginAt
                    );
                    return new UserLookup(userRepository.save(user), true);
                });
    }

    private record UserLookup(User user, boolean newUser) {
    }
}
