package com.shareway.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "visitors")
public class Visitor {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "user_name", length = 150)
    private String userName;

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "anonymous_id", length = 100)
    private String anonymousId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(length = 500)
    private String referrer;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "accepted_cookies")
    @Builder.Default
    private boolean acceptedCookies = false;

    @Column(name = "visited_at", nullable = false)
    @Builder.Default
    private LocalDateTime visitedAt = LocalDateTime.now();
}
