package com.pabom.backend.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.pabom.backend.auth.domain.model.OAuthProvider;
import com.pabom.backend.notification.application.port.CareTaskQueryPort;
import com.pabom.backend.notification.application.port.CareTaskQueryPort.PetDueTasks;
import com.pabom.backend.notification.application.port.PushSenderPort;
import com.pabom.backend.notification.application.port.PushSenderPort.SendTicket;
import com.pabom.backend.notification.domain.type.AppVariant;
import com.pabom.backend.notification.domain.type.PushPlatform;
import com.pabom.backend.notification.infrastructure.persistence.NotificationLogJpaRepository;
import com.pabom.backend.notification.infrastructure.persistence.PushTicketJpaRepository;
import com.pabom.backend.notification.infrastructure.persistence.PushTokenJpaRepository;
import com.pabom.backend.notification.application.command.RegisterPushTokenCommand;
import com.pabom.backend.user.domain.aggregate.User;
import com.pabom.backend.user.infrastructure.persistence.UserJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** 스케줄러가 같은 구간에 두 번(또는 서버 두 대에서) 돌아도 notification_logs UNIQUE로 하루 1건만 나간다. */
@SpringBootTest
@ActiveProfiles("test")
class CareReminderDuplicatePreventionIntegrationTest {

    /** 2026-11-21 09:05 Asia/Seoul */
    static final Instant NOW = Instant.parse("2026-11-21T00:05:00Z");

    @MockitoBean PushSenderPort sender;
    @MockitoBean CareTaskQueryPort careTasks;
    @Autowired CareReminderService careReminderService;
    @Autowired PushTokenService pushTokenService;
    @Autowired PushTokenJpaRepository pushTokens;
    @Autowired PushTicketJpaRepository pushTickets;
    @Autowired NotificationLogJpaRepository logs;
    @Autowired UserJpaRepository users;

    private Long userId;

    @AfterEach
    void cleanUp() {
        logs.deleteAll();
        pushTickets.deleteAll();
        pushTokens.deleteAll();
        if (userId != null) {
            users.deleteById(userId);
        }
    }

    @Test
    void sendsCareReminderOnlyOncePerMemberPerDay() {
        User user = User.pending(OAuthProvider.KAKAO, "care-reminder-dup", "닉네임", NOW);
        user.activate(NOW);
        userId = users.saveAndFlush(user).getId();
        pushTokenService.register(userId, new RegisterPushTokenCommand(
                "ExponentPushToken[dup]", PushPlatform.IOS, AppVariant.PRODUCTION));
        given(sender.isConfigured()).willReturn(true);
        given(careTasks.findDueTasks(any(), any()))
                .willReturn(List.of(new PetDueTasks(1L, "레오", List.of("급여"))));
        given(sender.send(anyList())).willReturn(List.of(SendTicket.ok("ticket-dup")));

        careReminderService.sendDueReminders();
        careReminderService.sendDueReminders();

        verify(sender, times(1)).send(anyList());
        assertThat(logs.findAll()).singleElement()
                .satisfies(log -> assertThat(log.getUserId()).isEqualTo(userId));
        assertThat(pushTickets.findAll()).singleElement()
                .satisfies(ticket -> assertThat(ticket.getTicketId()).isEqualTo("ticket-dup"));
    }

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
