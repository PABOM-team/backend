package com.pabom.backend.pet.application.command;

public record PatchValue<T>(boolean present, T value) {

    public static <T> PatchValue<T> absent() {
        return new PatchValue<>(false, null);
    }

    public static <T> PatchValue<T> of(T value) {
        return new PatchValue<>(true, value);
    }
}
