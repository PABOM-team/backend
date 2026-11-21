package com.pabom.backend.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.pabom.backend.notification.application.port.CareTaskQueryPort;
import com.pabom.backend.notification.application.port.CareTaskQueryPort.PetDueTasks;
import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.application.port.PushSenderPort.PushMessage;
import com.pabom.backend.notification.application.result.PushDeliveryResult;
import com.pabom.backend.notification.domain.entity.NotificationLog;
import com.pabom.backend.notification.domain.entity.NotificationSetting;
import com.pabom.backend.notification.domain.entity.PushToken;
import com.pabom.backend.notification.domain.repository.NotificationLogRepository;
import com.pabom.backend.notification.domain.repository.NotificationSettingRepository;
import com.pabom.backend.notification.domain.repository.PushTokenRepository;
import com.pabom.backend.notification.domain.type.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CareReminderServiceTest {

    /** 2026-11-21 09:10 Asia/Seoul */
    private static final Instant SEOUL_0910 = Instant.parse("2026-11-21T00:10:00Z");

    private PushSenderPort sender;
    private PushDeliveryService delivery;
    private PushTokenRepository tokens;
    private NotificationSettingRepository settings;
    private NotificationLogRepository logs;
    private CareTaskQueryPort careTasks;

    @BeforeEach
    void setUp() {
        sender = mock(PushSenderPort.class);
        delivery = mock(PushDeliveryService.class);
        tokens = mock(PushTokenRepository.class);
        settings = mock(NotificationSettingRepository.class);
        logs = mock(NotificationLogRepository.class);
        careTasks = mock(CareTaskQueryPort.class);
        given(sender.isConfigured()).willReturn(true);
        given(delivery.deliver(anyList())).willAnswer(call ->
                new PushDeliveryResult(((List<?>) call.getArgument(0)).size(), 0));
    }

    @Test
    void skipsWholeRunWhenAccessTokenIsMissing() {
        given(sender.isConfigured()).willReturn(false);

        assertThat(service(SEOUL_0910).sendDueReminders()).isEqualTo(PushDeliveryResult.empty());

        verifyNoInteractions(tokens, settings, logs, careTasks, delivery);
    }

    @Test
    void sendsOneCareReminderPerMemberToEveryDeviceWithCareChannelAndHomeUrl() {
        given(tokens.findDistinctUserIds()).willReturn(List.of(1L));
        given(settings.findAllByUserIdIn(List.of(1L))).willReturn(List.of());
        given(careTasks.findDueTasks(1L, LocalDate.of(2026, 11, 21)))
                .willReturn(List.of(new PetDueTasks(10L, "레오", List.of("급여"))));
        given(tokens.findAllByUserIdIn(List.of(1L)))
                .willReturn(List.of(token(1L, "ExponentPushToken[a]"), token(1L, "ExponentPushToken[b]")));

        service(SEOUL_0910).sendDueReminders();

        ArgumentCaptor<NotificationLog> log = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logs).saveAndFlush(log.capture());
        assertThat(log.getValue().getType()).isEqualTo(NotificationType.CARE_REMINDER);
        assertThat(log.getValue().getTargetDate()).isEqualTo(LocalDate.of(2026, 11, 21));
        List<PushMessage> messages = sentMessages();
        assertThat(messages).extracting(PushMessage::to)
                .containsExactly("ExponentPushToken[a]", "ExponentPushToken[b]");
        assertThat(messages).allSatisfy(message -> {
            assertThat(message.channelId()).isEqualTo("care");
            assertThat(message.data()).isEqualTo(Map.of("url", "/home"));
            assertThat(message.title()).isEqualTo("오늘의 케어 일정");
            assertThat(message.body()).isEqualTo("레오가 오늘 급여를 기다려요");
        });
    }

    @Test
    void usesMemberTimeZoneAndReminderSlot() {
        NotificationSetting newYorkNineThirtyPm = setting(2L, true, LocalTime.of(21, 30), "America/New_York");
        NotificationSetting seoulEvening = setting(3L, true, LocalTime.of(21, 0), "Asia/Seoul");
        NotificationSetting disabled = setting(4L, false, LocalTime.of(9, 0), "Asia/Seoul");
        given(tokens.findDistinctUserIds()).willReturn(List.of(2L, 3L, 4L));
        given(settings.findAllByUserIdIn(List.of(2L, 3L, 4L)))
                .willReturn(List.of(newYorkNineThirtyPm, seoulEvening, disabled));
        given(careTasks.findDueTasks(eq(2L), any()))
                .willReturn(List.of(new PetDueTasks(20L, "모찌", List.of("청소"))));
        given(tokens.findAllByUserIdIn(List.of(2L))).willReturn(List.of(token(2L, "ExpoPushToken[ny]")));

        // 2026-11-21 02:40 UTC = 2026-11-20 21:40 New York, 11:40 Seoul
        service(Instant.parse("2026-11-21T02:40:00Z")).sendDueReminders();

        verify(careTasks).findDueTasks(2L, LocalDate.of(2026, 11, 20));
        verify(careTasks, never()).findDueTasks(eq(3L), any());
        verify(careTasks, never()).findDueTasks(eq(4L), any());
        assertThat(sentMessages()).extracting(PushMessage::to).containsExactly("ExpoPushToken[ny]");
    }

    @Test
    void doesNotClaimOrSendWhenNothingIsDueToday() {
        given(tokens.findDistinctUserIds()).willReturn(List.of(1L));
        given(careTasks.findDueTasks(any(), any())).willReturn(List.of());

        service(SEOUL_0910).sendDueReminders();

        verifyNoInteractions(logs, delivery);
    }

    @Test
    void skipsMemberAlreadySentToday() {
        given(tokens.findDistinctUserIds()).willReturn(List.of(1L));
        given(careTasks.findDueTasks(any(), any()))
                .willReturn(List.of(new PetDueTasks(10L, "레오", List.of("급여"))));
        willThrow(new DataIntegrityViolationException("uk_notification_logs_user_type_date"))
                .given(logs).saveAndFlush(any());

        service(SEOUL_0910).sendDueReminders();

        verifyNoInteractions(delivery);
    }

    @Test
    void oneFailingMemberDoesNotBlockOthers() {
        given(tokens.findDistinctUserIds()).willReturn(List.of(1L, 2L));
        given(careTasks.findDueTasks(eq(1L), any())).willThrow(new IllegalStateException("boom"));
        given(careTasks.findDueTasks(eq(2L), any()))
                .willReturn(List.of(new PetDueTasks(20L, "모찌", List.of("급여"))));
        given(tokens.findAllByUserIdIn(List.of(2L))).willReturn(List.of(token(2L, "ExponentPushToken[m]")));

        service(SEOUL_0910).sendDueReminders();

        assertThat(sentMessages()).extracting(PushMessage::to).containsExactly("ExponentPushToken[m]");
    }

    @Test
    void reminderSlotMatchesOnlyItsThirtyMinuteWindow() {
        NotificationSetting nine = setting(1L, true, LocalTime.of(9, 0), "Asia/Seoul");
        assertThat(CareReminderService.isDue(nine, Instant.parse("2026-11-21T00:00:00Z"))).isTrue();
        assertThat(CareReminderService.isDue(nine, Instant.parse("2026-11-21T00:29:59Z"))).isTrue();
        assertThat(CareReminderService.isDue(nine, Instant.parse("2026-11-21T00:30:00Z"))).isFalse();
        assertThat(CareReminderService.isDue(nine, Instant.parse("2026-11-20T23:59:59Z"))).isFalse();
        NotificationSetting kathmandu = setting(2L, true, LocalTime.of(9, 0), "Asia/Kathmandu");
        // 03:30 UTC = 09:15 Kathmandu (+05:45)
        assertThat(CareReminderService.isDue(kathmandu, Instant.parse("2026-11-21T03:30:00Z"))).isTrue();
    }

    @SuppressWarnings("unchecked")
    private List<PushMessage> sentMessages() {
        ArgumentCaptor<List<PushMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(delivery).deliver(captor.capture());
        return captor.getValue();
    }

    private CareReminderService service(Instant now) {
        return new CareReminderService(sender, delivery, tokens, settings, logs, careTasks,
                new CareReminderMessageFactory(),
                new TransactionTemplate(mock(PlatformTransactionManager.class)),
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private NotificationSetting setting(Long userId, boolean enabled, LocalTime time, String zone) {
        NotificationSetting setting = NotificationSetting.defaults(userId, SEOUL_0910);
        setting.update(enabled, time, zone, SEOUL_0910);
        return setting;
    }

    private PushToken token(Long userId, String value) {
        PushToken token = org.springframework.beans.BeanUtils.instantiateClass(PushToken.class);
        ReflectionTestUtils.setField(token, "userId", userId);
        ReflectionTestUtils.setField(token, "token", value);
        return token;
    }
}
