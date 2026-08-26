package com.shareway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ride_rejections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RideRejection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "ride_id", nullable = false)
    private String rideId;

    @Column(name = "driver_id", nullable = false)
    private String driverId;

    @Column(name = "passenger_id")
    private String passengerId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
