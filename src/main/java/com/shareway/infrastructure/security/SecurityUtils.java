package com.shareway.infrastructure.security;

import com.shareway.domain.exception.NotAuthorizedException;
import com.shareway.domain.model.SystemRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new NotAuthorizedException("Not authenticated");
        return (String) auth.getPrincipal();
    }

    public static String currentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse(null);
    }

    /**
     * Rôle système (SUPER_ADMIN / ADMIN / MODERATOR / SUPPORT / NONE) de l'utilisateur
     * courant, résolu parmi toutes les autorités portées par le token.
     * Ne se fie PAS à la « première » autorité (qui est le rôle métier).
     */
    public static SystemRole currentSystemRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        for (SystemRole sr : SystemRole.values()) {
            boolean has = auth.getAuthorities().stream()
                    .anyMatch(a -> sr.toGrantedAuthority().equals(a.getAuthority()));
            if (has) return sr;
        }
        return null;
    }

    public static boolean isAdmin() {
        String role = currentUserRole();
        return "ADMIN".equals(role) || "SUPER_ADMIN".equals(role) || "MODERATOR".equals(role);
    }
}
