package com.shareway.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SmsProvider provider = SmsProvider.DISABLED;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "api_secret")
    private String apiSecret;

    @Column(name = "sender_number")
    private String senderNumber;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum SmsProvider {
        TWILIO, AFRICAS_TALKING, DISABLED
    }
}
