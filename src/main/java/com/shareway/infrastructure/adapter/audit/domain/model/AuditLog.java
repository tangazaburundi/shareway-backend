package com.shareway.infrastructure.adapter.audit.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "actor_id")
    private String actorId;
    @Column(name = "actor_email")
    private String actorEmail;
    @Column(name = "actor_role")
    private String actorRole;

    @Column(nullable = false)
    private String action;
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "old_value", columnDefinition = "JSON")
    private String oldValue;
    @Column(name = "new_value", columnDefinition = "JSON")
    private String newValue;

    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "user_agent")
    private String userAgent;

    @Builder.Default
    private boolean success = true;
    @Column(name = "error_message")
    private String errorMessage;
    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
