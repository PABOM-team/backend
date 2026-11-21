package com.pabom.backend.notification.application.service;

import com.pabom.backend.notification.application.port.CareTaskQueryPort.PetDueTasks;
import java.util.List;
import org.springframework.stereotype.Component;

/** 케어 알림 문구. 예: "레오가 오늘 급여·습식 은신처 적시기를 기다려요" */
@Component
public class CareReminderMessageFactory {

    static final String TITLE = "오늘의 케어 일정";
    private static final int MAX_ITEM_NAMES = 2;
    private static final int MAX_PET_NAMES = 2;

    public Content create(List<PetDueTasks> pets) {
        if (pets.isEmpty()) {
            throw new IllegalArgumentException("케어 알림 대상 개체가 없습니다.");
        }
        if (pets.size() == 1) {
            PetDueTasks pet = pets.get(0);
            String items = summarize(pet.itemNames(), MAX_ITEM_NAMES, "건");
            return new Content(TITLE, pet.petName() + subjectParticle(pet.petName())
                    + " 오늘 " + items + objectParticle(items) + " 기다려요");
        }
        int taskCount = pets.stream().mapToInt(pet -> pet.itemNames().size()).sum();
        String names = summarize(pets.stream().map(PetDueTasks::petName).toList(), MAX_PET_NAMES, "마리");
        return new Content(TITLE, names + "의 케어 " + taskCount + "건이 오늘 예정이에요");
    }

    private String summarize(List<String> names, int max, String unit) {
        String shown = String.join("·", names.subList(0, Math.min(max, names.size())));
        int rest = names.size() - max;
        return rest > 0 ? shown + " 외 " + rest + unit : shown;
    }

    private String subjectParticle(String word) {
        return switch (finalConsonant(word)) {
            case HAS -> "이";
            case NONE -> "가";
            case UNKNOWN -> "이(가)";
        };
    }

    private String objectParticle(String word) {
        return switch (finalConsonant(word)) {
            case HAS -> "을";
            case NONE -> "를";
            case UNKNOWN -> "을(를)";
        };
    }

    private FinalConsonant finalConsonant(String word) {
        if (word == null || word.isEmpty()) {
            return FinalConsonant.UNKNOWN;
        }
        char last = word.charAt(word.length() - 1);
        if (last >= '가' && last <= '힣') {
            return (last - '가') % 28 == 0 ? FinalConsonant.NONE : FinalConsonant.HAS;
        }
        if (Character.isDigit(last)) {
            return "013678".indexOf(last) >= 0 ? FinalConsonant.HAS : FinalConsonant.NONE;
        }
        return FinalConsonant.UNKNOWN;
    }

    private enum FinalConsonant {
        HAS,
        NONE,
        UNKNOWN
    }

    public record Content(String title, String body) {
    }
}
