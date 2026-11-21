package com.pabom.backend.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.notification.application.command.RegisterPushTokenCommand;
import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.error.NotificationErrorCode;
import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import com.pabom.backend.notification.infrastructure.persistence.PushTokenJpaRepository;
import com.pabom.backend.user.application.service.WithdrawalService;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushTokenServiceIntegrationTest {

    private static final String TOKEN = "ExponentPushToken[device-1]";

    @Autowired PushTokenJpaRepository pushTokens;
    @Autowired UserJpaRepository users;
    @Autowired WithdrawalService withdrawalService;
    @Autowired Clock clock;

    @Test
    void registersThenMovesExistingTokenToNewOwnerAndRefreshesLastSeen() {
        Long first = activeUser("first");
        Long second = activeUser("second");
        Instant t1 = Instant.parse("2026-11-21T00:00:00Z");
        Instant t2 = Instant.parse("2026-11-22T00:00:00Z");

        service(t1).register(first, command(TOKEN, PushPlatform.IOS, AppVariant.DEVELOPMENT));
        service(t1).register(first, command(TOKEN, PushPlatform.IOS, AppVariant.DEVELOPMENT));
        service(t2).register(second, command(TOKEN, PushPlatform.ANDROID, AppVariant.PRODUCTION));

        assertThat(pushTokens.findAll()).singleElement().satisfies(token -> {
            assertThat(token.getUserId()).isEqualTo(second);
            assertThat(token.getPlatform()).isEqualTo(PushPlatform.ANDROID);
            assertThat(token.getAppVariant()).isEqualTo(AppVariant.PRODUCTION);
            assertThat(token.getCreatedAt()).isEqualTo(t1);
            assertThat(token.getLastSeenAt()).isEqualTo(t2);
        });
    }

    @Test
    void keepsSeveralDevicesPerUser() {
        Long userId = activeUser("multi");
        PushTokenService service = service(clock.instant());

        service.register(userId, command("ExponentPushToken[phone]", PushPlatform.IOS, AppVariant.PRODUCTION));
        service.register(userId, command("ExpoPushToken[tablet]", PushPlatform.ANDROID, AppVariant.PRODUCTION));

        assertThat(pushTokens.findAllByUserIdOrderByIdAsc(userId)).extracting(PushToken::getToken)
                .containsExactly("ExponentPushToken[phone]", "ExpoPushToken[tablet]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"fcm-token", "ExponentPushToken[]", "ExponentPushToken[a b]",
            "ExponentPushToken[abc", "xExponentPushToken[abc]", "ExpoPushToken[abc]x"})
    void rejectsNonExpoTokenFormats(String token) {
        Long userId = activeUser("invalid-" + Math.abs(token.hashCode()));

        assertThatThrownBy(() -> service(clock.instant())
                .register(userId, command(token, PushPlatform.IOS, AppVariant.PRODUCTION)))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).errorCode())
                .isEqualTo(NotificationErrorCode.INVALID_PUSH_TOKEN);
        assertThat(pushTokens.findAll()).isEmpty();
    }

    @Test
    void unregistersOnlyOwnTokenAndIsIdempotent() {
        Long owner = activeUser("owner");
        Long other = activeUser("other");
        PushTokenService service = service(clock.instant());
        service.register(owner, command(TOKEN, PushPlatform.IOS, AppVariant.PRODUCTION));

        service.unregister(other, TOKEN);
        assertThat(pushTokens.findByToken(TOKEN)).isPresent();

        service.unregister(owner, TOKEN);
        service.unregister(owner, TOKEN);
        service.unregister(owner, "ExponentPushToken[never-registered]");
        assertThat(pushTokens.findByToken(TOKEN)).isEmpty();
    }

    @Test
    void withdrawalDeletesAllTokensOfMemberImmediately() {
        Long leaving = activeUser("leaving");
        Long staying = activeUser("staying");
        PushTokenService service = service(clock.instant());
        service.register(leaving, command("ExponentPushToken[a]", PushPlatform.IOS, AppVariant.PRODUCTION));
        service.register(leaving, command("ExponentPushToken[b]", PushPlatform.ANDROID, AppVariant.PRODUCTION));
        service.register(staying, command("ExponentPushToken[c]", PushPlatform.IOS, AppVariant.PRODUCTION));

        withdrawalService.withdraw(leaving);

        assertThat(pushTokens.findAll()).extracting(PushToken::getToken)
                .containsExactly("ExponentPushToken[c]");
    }

    private PushTokenService service(Instant now) {
        return new PushTokenService(
                new com.pabom.backend.notification.infrastructure.persistence.PushTokenPersistenceAdapter(pushTokens),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private RegisterPushTokenCommand command(String token, PushPlatform platform, AppVariant variant) {
        return new RegisterPushTokenCommand(token, platform, variant);
    }

    private Long activeUser(String providerId) {
        Instant now = clock.instant();
        User user = User.pending(OAuthProvider.KAKAO, "push-" + providerId, "닉네임", now);
        user.activate(now);
        return users.saveAndFlush(user).getId();
    }
}
