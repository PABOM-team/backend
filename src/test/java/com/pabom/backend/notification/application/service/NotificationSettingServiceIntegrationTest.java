package com.pabom.backend.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pabom.backend.auth.domain.error.AuthErrorCode;
import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.global.error.BusinessException;
import com.pabom.backend.global.error.GlobalExceptionHandler.FieldErrorDetail;
import com.pabom.backend.notification.application.command.UpdateNotificationSettingCommand;
import com.pabom.backend.notification.application.result.NotificationSettingResult;
import com.pabom.backend.notification.infrastructure.persistence.NotificationSettingJpaRepository;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Clock;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationSettingServiceIntegrationTest {

    @Autowired NotificationSettingService service;
    @Autowired NotificationSettingJpaRepository settings;
    @Autowired UserJpaRepository users;
    @Autowired Clock clock;

    @Test
    void returnsDefaultsWithoutCreatingRow() {
        Long userId = activeUser("defaults");

        assertThat(service.get(userId))
                .isEqualTo(new NotificationSettingResult(true, LocalTime.of(9, 0), "Asia/Seoul"));
        assertThat(settings.findById(userId)).isEmpty();
    }

    @Test
    void patchesOnlyGivenFieldsAndKeepsOthers() {
        Long userId = activeUser("patch");

        NotificationSettingResult timeChanged = service.update(userId,
                new UpdateNotificationSettingCommand(null, LocalTime.of(21, 30), null));
        assertThat(timeChanged)
                .isEqualTo(new NotificationSettingResult(true, LocalTime.of(21, 30), "Asia/Seoul"));

        NotificationSettingResult disabled = service.update(userId,
                new UpdateNotificationSettingCommand(false, null, "America/New_York"));
        assertThat(disabled)
                .isEqualTo(new NotificationSettingResult(false, LocalTime.of(21, 30), "America/New_York"));
        assertThat(service.get(userId)).isEqualTo(disabled);

        assertThat(service.update(userId, new UpdateNotificationSettingCommand(null, null, null)))
                .isEqualTo(disabled);
    }

    @Test
    void rejectsNonIanaTimeZonesAndOffSlotTimes() {
        Long userId = activeUser("invalid");
        for (String zone : new String[] {"KST", "+09:00", "UTC+9", "Seoul", ""}) {
            assertValidationFailed(() -> service.update(userId,
                    new UpdateNotificationSettingCommand(null, null, zone)), "timeZone");
        }
        assertValidationFailed(() -> service.update(userId,
                new UpdateNotificationSettingCommand(null, LocalTime.of(9, 15), null)), "careReminderTime");
        assertThat(settings.findById(userId)).isEmpty();
    }

    private void assertValidationFailed(Runnable call, String field) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.errorCode()).isEqualTo(AuthErrorCode.VALIDATION_FAILED);
                    assertThat(((FieldErrorDetail) error.details()).field()).isEqualTo(field);
                });
    }

    private Long activeUser(String providerId) {
        User user = User.pending(OAuthProvider.GOOGLE, "setting-" + providerId, "닉네임", clock.instant());
        user.activate(clock.instant());
        return users.saveAndFlush(user).getId();
    }
}
