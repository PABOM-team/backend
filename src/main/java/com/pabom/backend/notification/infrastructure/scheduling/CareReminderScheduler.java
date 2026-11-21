package com.pabom.backend.notification.infrastructure.scheduling;

import com.pabom.backend.notification.application.service.CareReminderService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CareReminderScheduler {

    private final CareReminderService careReminderService;

    /** 매시 00분·30분. 알림 시각 단위(30분)와 같아야 한다. */
    @Scheduled(cron = "${app.push.care-reminder.cron:0 0/30 * * * *}", zone = "UTC")
    public void sendCareReminders() {
        careReminderService.sendDueReminders();
    }
}
