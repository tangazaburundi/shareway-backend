-- ============================================================
-- SHAREWAY - V1 : Schéma complet avec types corrects
-- ============================================================

CREATE TABLE users (
    id                   VARCHAR(255) NOT NULL DEFAULT (UUID()),
    first_name           VARCHAR(100) NOT NULL,
    last_name            VARCHAR(100) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    phone                VARCHAR(30),
    phone_visible        TINYINT(1)   NOT NULL DEFAULT 1,
    avatar_url           VARCHAR(500),
    bio                  TEXT,
    role                 ENUM('DRIVER','PASSENGER','BOTH') NOT NULL DEFAULT 'PASSENGER',
    preferred_lang       ENUM('fr','ki') NOT NULL DEFAULT 'fr',
    is_active            TINYINT(1)   NOT NULL DEFAULT 1,
    is_blocked           TINYINT(1)   NOT NULL DEFAULT 0,
    block_reason         TEXT,
    blocked_at           DATETIME,
    blocked_by_id        VARCHAR(255),
    email_verified       TINYINT(1)   NOT NULL DEFAULT 0,
    email_verify_token   VARCHAR(255),
    email_verify_expiry  DATETIME,
    phone_verified       TINYINT(1)   NOT NULL DEFAULT 0,
    phone_otp            VARCHAR(10),
    phone_otp_expiry     DATETIME,
    identity_verified    TINYINT(1)   NOT NULL DEFAULT 0,
    identity_verified_at DATETIME,
    identity_verified_by VARCHAR(255),
    two_fa_enabled       TINYINT(1)   NOT NULL DEFAULT 0,
    two_fa_secret        VARCHAR(255),
    deleted_at           DATETIME,
    deleted_by           VARCHAR(255),
    rating               DECIMAL(3,2) DEFAULT 0.00,
    review_count         INT          NOT NULL DEFAULT 0,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at        DATETIME,
    PRIMARY KEY (id),
    INDEX idx_users_email (email),
    INDEX idx_users_role (role),
    INDEX idx_users_active (is_active),
    INDEX idx_users_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_travel_preferences (
    user_id  VARCHAR(255) NOT NULL,
    music    TINYINT(1)   NOT NULL DEFAULT 0,
    smoking  TINYINT(1)   NOT NULL DEFAULT 0,
    pets     TINYINT(1)   NOT NULL DEFAULT 0,
    talking  TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicles (
    id            VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id       VARCHAR(255) NOT NULL,
    brand         VARCHAR(100) NOT NULL,
    model         VARCHAR(100) NOT NULL,
    color         VARCHAR(50)  NOT NULL,
    license_plate VARCHAR(20)  NOT NULL,
    year          SMALLINT,
    photo_url     VARCHAR(500),
    is_active     TINYINT(1)   NOT NULL DEFAULT 1,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_vehicles_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE trips (
    id                 VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    driver_id          VARCHAR(255)  NOT NULL,
    departure_city     VARCHAR(150)  NOT NULL,
    arrival_city       VARCHAR(150)  NOT NULL,
    departure_address  VARCHAR(500),
    arrival_address    VARCHAR(500),
    departure_lat      DECIMAL(10,7),
    departure_lng      DECIMAL(10,7),
    arrival_lat        DECIMAL(10,7),
    arrival_lng        DECIMAL(10,7),
    departure_time     DATETIME      NOT NULL,
    arrival_time       DATETIME,
    total_seats        INT           NOT NULL,
    available_seats    INT           NOT NULL,
    price_per_seat     DECIMAL(12,2) NOT NULL,
    currency           ENUM('FBU','USD','EUR') NOT NULL DEFAULT 'FBU',
    description        TEXT,
    status             ENUM('OPEN','FULL','CANCELLED','COMPLETED') NOT NULL DEFAULT 'OPEN',
    share_token        VARCHAR(100)  UNIQUE,
    is_recurring       TINYINT(1)    NOT NULL DEFAULT 0,
    frequency          ENUM('ONCE','WEEKLY','BIWEEKLY','MONTHLY'),
    recurring_days     VARCHAR(50),
    recurring_end_date DATE,
    parent_trip_id     VARCHAR(255),
    current_lat        DECIMAL(10,7),
    current_lng        DECIMAL(10,7),
    tracking_enabled   TINYINT(1)    NOT NULL DEFAULT 0,
    deleted_at         DATETIME,
    deleted_by         VARCHAR(255),
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (driver_id) REFERENCES users(id),
    FOREIGN KEY (parent_trip_id) REFERENCES trips(id) ON DELETE SET NULL,
    INDEX idx_trips_driver (driver_id),
    INDEX idx_trips_departure (departure_city, arrival_city),
    INDEX idx_trips_time (departure_time),
    INDEX idx_trips_status (status),
    INDEX idx_trips_share (share_token),
    INDEX idx_trips_deleted (deleted_at),
    FULLTEXT idx_trips_cities (departure_city, arrival_city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE trip_preferences (
    trip_id          VARCHAR(255) NOT NULL,
    music            TINYINT(1)   NOT NULL DEFAULT 0,
    smoking          TINYINT(1)   NOT NULL DEFAULT 0,
    pets             TINYINT(1)   NOT NULL DEFAULT 0,
    talking          TINYINT(1)   NOT NULL DEFAULT 0,
    air_conditioning TINYINT(1)   NOT NULL DEFAULT 0,
    PRIMARY KEY (trip_id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE stop_points (
    id           VARCHAR(255) NOT NULL DEFAULT (UUID()),
    trip_id      VARCHAR(255) NOT NULL,
    city         VARCHAR(150) NOT NULL,
    address      VARCHAR(500),
    lat          DECIMAL(10,7),
    lng          DECIMAL(10,7),
    stop_order   INT          NOT NULL,
    arrival_time DATETIME,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    INDEX idx_stop_points_trip (trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bookings (
    id                       VARCHAR(255) NOT NULL DEFAULT (UUID()),
    trip_id                  VARCHAR(255) NOT NULL,
    passenger_id             VARCHAR(255) NOT NULL,
    seats_booked             INT          NOT NULL DEFAULT 1,
    status                   ENUM('PENDING','CONFIRMED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'CONFIRMED',
    cancel_reason            TEXT,
    notify_driver            TINYINT(1)   NOT NULL DEFAULT 1,
    currency                 ENUM('FBU','USD','EUR') NOT NULL DEFAULT 'FBU',
    amount_paid              DECIMAL(12,2),
    stripe_payment_intent_id VARCHAR(255),
    stripe_status            VARCHAR(50),
    deleted_at               DATETIME,
    created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trips(id),
    FOREIGN KEY (passenger_id) REFERENCES users(id),
    UNIQUE KEY uk_booking_trip_passenger (trip_id, passenger_id),
    INDEX idx_bookings_passenger (passenger_id),
    INDEX idx_bookings_status (status),
    INDEX idx_bookings_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id             VARCHAR(255) NOT NULL DEFAULT (UUID()),
    trip_id        VARCHAR(255) NOT NULL,
    author_id      VARCHAR(255) NOT NULL,
    target_user_id VARCHAR(255) NOT NULL,
    rating         INT          NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment        TEXT,
    type           ENUM('DRIVER_TO_PASSENGER','PASSENGER_TO_DRIVER') NOT NULL,
    is_flagged     TINYINT(1)   NOT NULL DEFAULT 0,
    flag_reason    TEXT,
    is_approved    TINYINT(1)   NOT NULL DEFAULT 1,
    moderated_at   DATETIME,
    moderated_by   VARCHAR(255),
    deleted_at     DATETIME,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trips(id),
    FOREIGN KEY (author_id) REFERENCES users(id),
    FOREIGN KEY (target_user_id) REFERENCES users(id),
    UNIQUE KEY uk_review_trip_author_target (trip_id, author_id, target_user_id),
    INDEX idx_reviews_target (target_user_id),
    INDEX idx_reviews_flagged (is_flagged),
    INDEX idx_reviews_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE messages (
    id          VARCHAR(255) NOT NULL DEFAULT (UUID()),
    sender_id   VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    is_read     TINYINT(1)   NOT NULL DEFAULT 0,
    read_at     DATETIME,
    is_flagged  TINYINT(1)   NOT NULL DEFAULT 0,
    flag_reason TEXT,
    deleted_at  DATETIME,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (sender_id) REFERENCES users(id),
    FOREIGN KEY (receiver_id) REFERENCES users(id),
    INDEX idx_messages_sender (sender_id),
    INDEX idx_messages_receiver (receiver_id),
    INDEX idx_messages_conversation (sender_id, receiver_id),
    INDEX idx_messages_flagged (is_flagged),
    INDEX idx_messages_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id         VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id    VARCHAR(255) NOT NULL,
    type       ENUM('BOOKING','CANCELLATION','MESSAGE','REVIEW','SYSTEM','PAYMENT','TRIP_UPDATE','IDENTITY_VERIFIED') NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    link       VARCHAR(500),
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    read_at    DATETIME,
    metadata   JSON,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_read (user_id, is_read),
    INDEX idx_notifications_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_documents (
    id               VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id          VARCHAR(255) NOT NULL,
    type             ENUM('ID_CARD','PASSPORT','DRIVER_LICENSE','VEHICLE_REGISTRATION','INSURANCE') NOT NULL,
    file_url         VARCHAR(500) NOT NULL,
    file_name        VARCHAR(255),
    file_size        BIGINT,
    mime_type        VARCHAR(100),
    status           ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason TEXT,
    reviewed_at      DATETIME,
    reviewed_by      VARCHAR(255),
    expires_at       DATE,
    deleted_at       DATETIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_documents_user (user_id),
    INDEX idx_documents_status (status),
    INDEX idx_documents_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id          VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id     VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  DATETIME     NOT NULL,
    is_revoked  TINYINT(1)   NOT NULL DEFAULT 0,
    device_info VARCHAR(500),
    ip_address  VARCHAR(45),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user (user_id),
    INDEX idx_refresh_tokens_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE two_fa_backup_codes (
    id         VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id    VARCHAR(255) NOT NULL,
    code_hash  VARCHAR(255) NOT NULL,
    is_used    TINYINT(1)   NOT NULL DEFAULT 0,
    used_at    DATETIME,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE login_history (
    id          VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id     VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(45)  NOT NULL,
    user_agent  VARCHAR(500),
    device_type VARCHAR(100),
    location    VARCHAR(255),
    success     TINYINT(1)   NOT NULL DEFAULT 1,
    fail_reason VARCHAR(255),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_login_history_user (user_id),
    INDEX idx_login_history_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE password_reset_tokens (
    id         VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id    VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME     NOT NULL,
    is_used    TINYINT(1)   NOT NULL DEFAULT 0,
    used_at    DATETIME,
    ip_address VARCHAR(45),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_favorites (
    id               VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id          VARCHAR(255) NOT NULL,
    favorite_user_id VARCHAR(255) NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorites (user_id, favorite_user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (favorite_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_favorites_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_blacklist (
    id              VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id         VARCHAR(255) NOT NULL,
    blocked_user_id VARCHAR(255) NOT NULL,
    reason          VARCHAR(500),
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_blacklist (user_id, blocked_user_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (blocked_user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reports (
    id           VARCHAR(255) NOT NULL DEFAULT (UUID()),
    reporter_id  VARCHAR(255) NOT NULL,
    target_type  ENUM('USER','TRIP','REVIEW','MESSAGE') NOT NULL,
    target_id    VARCHAR(255) NOT NULL,
    reason       ENUM('SPAM','HARASSMENT','INAPPROPRIATE_CONTENT','FRAUD','FAKE_PROFILE','DANGEROUS_DRIVING','OTHER') NOT NULL,
    description  TEXT,
    status       ENUM('PENDING','REVIEWED','DISMISSED','ACTIONED') NOT NULL DEFAULT 'PENDING',
    reviewed_by  VARCHAR(255),
    reviewed_at  DATETIME,
    action_taken VARCHAR(500),
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    INDEX idx_reports_target (target_type, target_id),
    INDEX idx_reports_status (status),
    INDEX idx_reports_reporter (reporter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupons (
    id                VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    code              VARCHAR(50)   NOT NULL UNIQUE,
    description       VARCHAR(255),
    discount_type     ENUM('PERCENT','FIXED') NOT NULL,
    discount_value    DECIMAL(10,2) NOT NULL,
    min_trip_amount   DECIMAL(10,2),
    max_discount      DECIMAL(10,2),
    currency          ENUM('FBU','USD','EUR') DEFAULT 'FBU',
    max_uses          INT,
    current_uses      INT           NOT NULL DEFAULT 0,
    max_uses_per_user INT           NOT NULL DEFAULT 1,
    is_active         TINYINT(1)    NOT NULL DEFAULT 1,
    starts_at         DATETIME,
    expires_at        DATETIME,
    created_by        VARCHAR(255),
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_coupons_code (code),
    INDEX idx_coupons_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coupon_usages (
    id               VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    coupon_id        VARCHAR(255)  NOT NULL,
    user_id          VARCHAR(255)  NOT NULL,
    booking_id       VARCHAR(255)  NOT NULL,
    discount_applied DECIMAL(10,2) NOT NULL,
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_booking (coupon_id, booking_id),
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE trip_locations (
    id         VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    trip_id    VARCHAR(255)  NOT NULL,
    driver_id  VARCHAR(255)  NOT NULL,
    lat        DECIMAL(10,7) NOT NULL,
    lng        DECIMAL(10,7) NOT NULL,
    speed      DECIMAL(6,2),
    heading    DECIMAL(5,2),
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    INDEX idx_trip_locations_trip (trip_id),
    INDEX idx_trip_locations_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id                       VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    booking_id               VARCHAR(255)  NOT NULL,
    user_id                  VARCHAR(255)  NOT NULL,
    amount                   DECIMAL(12,2) NOT NULL,
    currency                 ENUM('FBU','USD','EUR') NOT NULL,
    stripe_payment_intent_id VARCHAR(255)  UNIQUE,
    stripe_charge_id         VARCHAR(255),
    stripe_customer_id       VARCHAR(255),
    status                   ENUM('PENDING','PROCESSING','SUCCEEDED','FAILED','REFUNDED','PARTIALLY_REFUNDED') NOT NULL DEFAULT 'PENDING',
    refund_amount            DECIMAL(12,2),
    refund_reason            TEXT,
    metadata                 JSON,
    created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_payments_user (user_id),
    INDEX idx_payments_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id            VARCHAR(255) NOT NULL DEFAULT (UUID()),
    actor_id      VARCHAR(255),
    actor_email   VARCHAR(255),
    actor_role    VARCHAR(50),
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     VARCHAR(255),
    old_value     JSON,
    new_value     JSON,
    ip_address    VARCHAR(45),
    user_agent    VARCHAR(500),
    success       TINYINT(1)   NOT NULL DEFAULT 1,
    error_message TEXT,
    duration_ms   INT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_stats (
    id                VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    stat_date         DATE          NOT NULL UNIQUE,
    new_users         INT           NOT NULL DEFAULT 0,
    active_users      INT           NOT NULL DEFAULT 0,
    new_trips         INT           NOT NULL DEFAULT 0,
    completed_trips   INT           NOT NULL DEFAULT 0,
    cancelled_trips   INT           NOT NULL DEFAULT 0,
    total_bookings    INT           NOT NULL DEFAULT 0,
    total_revenue_fbu DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_revenue_usd DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_revenue_eur DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_messages    INT           NOT NULL DEFAULT 0,
    total_reviews     INT           NOT NULL DEFAULT 0,
    avg_trip_rating   DECIMAL(3,2),
    total_km_estimated DECIMAL(12,2) NOT NULL DEFAULT 0,
    calculated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_daily_stats_date (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE driver_earnings (
    id           VARCHAR(255)  NOT NULL DEFAULT (UUID()),
    driver_id    VARCHAR(255)  NOT NULL,
    trip_id      VARCHAR(255)  NOT NULL,
    booking_id   VARCHAR(255)  NOT NULL,
    gross_amount DECIMAL(12,2) NOT NULL,
    platform_fee DECIMAL(12,2) NOT NULL DEFAULT 0,
    net_amount   DECIMAL(12,2) NOT NULL,
    currency     ENUM('FBU','USD','EUR') NOT NULL,
    status       ENUM('PENDING','CONFIRMED','PAID') NOT NULL DEFAULT 'PENDING',
    paid_at      DATETIME,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (driver_id) REFERENCES users(id),
    FOREIGN KEY (trip_id) REFERENCES trips(id),
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    INDEX idx_driver_earnings_driver (driver_id),
    INDEX idx_driver_earnings_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_sessions (
    id         VARCHAR(255) NOT NULL DEFAULT (UUID()),
    admin_id   VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    ip_address VARCHAR(45),
    expires_at DATETIME     NOT NULL,
    is_active  TINYINT(1)   NOT NULL DEFAULT 1,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (admin_id) REFERENCES users(id),
    INDEX idx_admin_sessions_admin (admin_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE admin_roles (
    id         VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id    VARCHAR(255) NOT NULL UNIQUE,
    role       ENUM('SUPER_ADMIN','ADMIN','MODERATOR','SUPPORT') NOT NULL DEFAULT 'SUPPORT',
    permissions JSON,
    granted_by VARCHAR(255),
    granted_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admin_roles_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE system_settings (
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT         NOT NULL,
    description   VARCHAR(500),
    updated_by    VARCHAR(255),
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('platform_fee_percent', '5.00', 'Commission plateforme en %'),
('max_seats_per_trip', '8', 'Nombre maximum de places par trajet'),
('min_price_fbu', '1000', 'Prix minimum par siège en FBU'),
('max_price_fbu', '500000', 'Prix maximum par siège en FBU'),
('review_window_days', '7', 'Jours après trajet pour laisser un avis'),
('max_active_trips_driver', '5', 'Nombre max trajets actifs par conducteur'),
('maintenance_mode', 'false', 'Mode maintenance'),
('registration_enabled', 'true', 'Inscriptions activées');