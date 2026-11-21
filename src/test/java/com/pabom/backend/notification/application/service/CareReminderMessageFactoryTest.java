package com.pabom.backend.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.pabom.backend.notification.application.port.CareTaskQueryPort.PetDueTasks;
import java.util.List;
import org.junit.jupiter.api.Test;

class CareReminderMessageFactoryTest {

    private final CareReminderMessageFactory factory = new CareReminderMessageFactory();

    @Test
    void namesOnePetAndItsItemsWithCorrectParticles() {
        assertThat(factory.create(List.of(pet("레오", "급여", "습식 은신처 적시기"))).body())
                .isEqualTo("레오가 오늘 급여·습식 은신처 적시기를 기다려요");
        assertThat(factory.create(List.of(pet("콩떡", "체중 측정"))).body())
                .isEqualTo("콩떡이 오늘 체중 측정을 기다려요");
        assertThat(factory.create(List.of(pet("Leo", "급여"))).body())
                .isEqualTo("Leo이(가) 오늘 급여를 기다려요");
    }

    @Test
    void summarizesManyItemsAndManyPets() {
        assertThat(factory.create(List.of(pet("레오", "급여", "탈피 확인", "청소", "체중 측정"))).body())
                .isEqualTo("레오가 오늘 급여·탈피 확인 외 2건을 기다려요");
        assertThat(factory.create(List.of(pet("레오", "급여"), pet("모찌", "급여", "청소"),
                        pet("콩떡", "체중 측정"))))
                .satisfies(content -> {
                    assertThat(content.title()).isEqualTo("오늘의 케어 일정");
                    assertThat(content.body()).isEqualTo("레오·모찌 외 1마리의 케어 4건이 오늘 예정이에요");
                });
    }

    private PetDueTasks pet(String name, String... items) {
        return new PetDueTasks(1L, name, List.of(items));
    }
}
