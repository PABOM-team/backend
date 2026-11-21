package com.pabom.backend.notification.infrastructure.care;

import com.pabom.backend.careguideline.application.result.TodayResult;
import com.pabom.backend.careguideline.application.result.TodayTaskStatus;
import com.pabom.backend.careguideline.application.service.TodayQueryService;
import com.pabom.backend.notification.application.port.CareTaskQueryPort;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 홈 화면 "오늘 할 일"과 같은 기준(nextDueOn, 미룬 경우 postponedTo)으로 그날 예정 항목을 고른다. */
@Component
@RequiredArgsConstructor
public class TodayCareTaskAdapter implements CareTaskQueryPort {

    private final TodayQueryService todayQueryService;

    @Override
    public List<PetDueTasks> findDueTasks(Long userId, LocalDate date) {
        TodayResult today = todayQueryService.getForDate(userId, date);
        return today.pets().stream()
                .map(pet -> new PetDueTasks(pet.petId(), pet.name(), pet.tasks().stream()
                        .filter(task -> task.status() == TodayTaskStatus.DUE)
                        .map(TodayResult.Task::itemName)
                        .toList()))
                .filter(pet -> !pet.itemNames().isEmpty())
                .toList();
    }
}
