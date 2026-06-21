package com.shareway.infrastructure.adapter.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SystemRole role = SystemRole.SUPPORT;

    @Column(columnDefinition = "JSON")
    private String permissions;

    @Column(name = "granted_by")
    private String grantedBy;

    @Column(name = "granted_at", updatable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    public void prePersist() {
        grantedAt = LocalDateTime.now();
    }

    // domain/model/AdminRole.java — ajouter cette méthode
    public boolean hasPermission(String permission) {
        if (this.role == SystemRole.SUPER_ADMIN) return true;  // SUPER_ADMIN passe toujours
        if (this.permissions == null) return false;
        return this.permissions.contains("ALL")
                || this.permissions.contains(permission);
    }
}