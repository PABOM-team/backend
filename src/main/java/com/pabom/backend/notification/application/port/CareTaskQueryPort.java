package com.pabom.backend.notification.application.port;

import java.time.LocalDate;
import java.util.List;

/** 케어 알림 대상(그날 예정인 루틴 항목)을 조회하는 포트. */
public interface CareTaskQueryPort {

    /** 회원의 활성 개체별로, 해당 날짜에 예정(DUE)인 관리 항목 이름을 돌려준다. 항목이 없는 개체는 빠진다. */
    List<PetDueTasks> findDueTasks(Long userId, LocalDate date);

    record PetDueTasks(Long petId, String petName, List<String> itemNames) {
    }
}
