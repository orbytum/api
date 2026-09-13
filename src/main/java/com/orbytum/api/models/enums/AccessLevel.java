package com.orbytum.api.models.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum AccessLevel {

    ADMIN("admin"),
    USER("user"),
    INITIAL_ADMIN("initial_admin");

    @Getter
    private String key;

    public static AccessLevel fromKey(String key) {
        for (AccessLevel level : AccessLevel.values()) {
            if (level.getKey().equalsIgnoreCase(key)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Nível de acesso inválido: " + key);
    }
}
