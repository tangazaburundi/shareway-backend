package com.shareway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sound_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSoundPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "ride_request_sound", nullable = false, length = 50)
    @Builder.Default
    private String rideRequestSound = "classic";

    @Column(name = "ride_accepted_sound", nullable = false, length = 50)
    @Builder.Default
    private String rideAcceptedSound = "success";

    @Column(name = "ride_cancelled_sound", nullable = false, length = 50)
    @Builder.Default
    private String rideCancelledSound = "alert";

    @Column(name = "ride_completed_sound", nullable = false, length = 50)
    @Builder.Default
    private String rideCompletedSound = "tada";

    @Column(name = "ride_rendered_sound", nullable = false, length = 50)
    @Builder.Default
    private String rideRenderedSound = "transfer";

    @Column(name = "message_sound", nullable = false, length = 50)
    @Builder.Default
    private String messageSound = "ping";

    @Column(name = "sos_sound", nullable = false, length = 50)
    @Builder.Default
    private String sosSound = "siren";

    @Column(name = "notification_volume", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal notificationVolume = new BigDecimal("0.30");

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
}
