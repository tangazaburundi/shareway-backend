package com.shareway.domain.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum SystemRole {
    SUPER_ADMIN, ADMIN, MODERATOR, SUPPORT;

    public static SystemRole fromString(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Le rôle ne peut pas être vide");
        }
        try {
            return SystemRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Rôle invalide : '" + role + "'. Valeurs acceptées : " +
                            Arrays.stream(SystemRole.values())
                                    .map(SystemRole::name)
                                    .collect(Collectors.joining(", "))
            );
        }
    }

    public String toGrantedAuthority() {
        return "ROLE_" + this.name();
    }
}