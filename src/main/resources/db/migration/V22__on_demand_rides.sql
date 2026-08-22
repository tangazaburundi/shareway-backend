-- ============================================================
-- V22 : Mode Uber — Courses on-demand
-- ============================================================
-- Note: FK constraints omitted intentionally.
-- JPA/Hibernate handles referential integrity at application level.
-- This avoids type mismatches with users.id (which may be
-- VARCHAR(255), CHAR(36), or altered by ddl-auto=update).
-- ============================================================

-- 1. Disponibilité du chauffeur
CREATE TABLE driver_availability (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    is_available TINYINT(1) NOT NULL DEFAULT 0,
    status ENUM('OFFLINE', 'AVAILABLE', 'BUSY', 'ON_TRIP') NOT NULL DEFAULT 'OFFLINE',
    current_lat DECIMAL(10, 8),
    current_lng DECIMAL(11, 8),
    last_location_update DATETIME,
    current_heading SMALLINT,
    max_distance_km INT NOT NULL DEFAULT 15,
    auto_accept TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE (user_id)
) ENGINE=InnoDB;

-- 2. Demandes de course on-demand
CREATE TABLE ride_requests (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    passenger_id VARCHAR(255) NOT NULL,
    driver_id VARCHAR(255),

    pickup_address VARCHAR(255),
    pickup_lat DECIMAL(10, 8) NOT NULL,
    pickup_lng DECIMAL(11, 8) NOT NULL,

    destination_address VARCHAR(255),
    destination_lat DECIMAL(10, 8) NOT NULL,
    destination_lng DECIMAL(11, 8) NOT NULL,

    estimated_distance_km DECIMAL(6, 2),
    estimated_duration_min INT,
    estimated_price DECIMAL(10, 2),
    final_price DECIMAL(10, 2),
    currency ENUM('FBU', 'USD', 'EUR') NOT NULL DEFAULT 'FBU',

    status ENUM('SEARCHING', 'DRIVER_FOUND', 'ACCEPTED', 'DRIVER_EN_ROUTE', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED') NOT NULL DEFAULT 'SEARCHING',

    cancelled_by ENUM('PASSENGER', 'DRIVER'),
    cancel_reason VARCHAR(500),

    search_started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    search_timeout_at DATETIME,
    driver_notified_at DATETIME,
    driver_responded_at DATETIME,
    pickup_at DATETIME,
    started_at DATETIME,
    completed_at DATETIME,

    stripe_payment_intent_id VARCHAR(255),
    payment_status ENUM('PENDING', 'AUTHORIZED', 'CAPTURED', 'REFUNDED', 'FAILED') DEFAULT 'PENDING',

    platform_fee_percent DECIMAL(5, 2) NOT NULL DEFAULT 15.00,
    platform_fee_amount DECIMAL(10, 2),
    driver_earnings DECIMAL(10, 2),

    notes VARCHAR(500),
    passenger_count INT NOT NULL DEFAULT 1,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_ride_status (status),
    INDEX idx_ride_passenger (passenger_id),
    INDEX idx_ride_driver (driver_id),
    INDEX idx_ride_search (status, search_started_at)
) ENGINE=InnoDB;

-- 3. Tracking GPS pendant la course
CREATE TABLE ride_tracking (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    ride_request_id VARCHAR(255) NOT NULL,
    lat DECIMAL(10, 8) NOT NULL,
    lng DECIMAL(11, 8) NOT NULL,
    heading SMALLINT,
    speed DECIMAL(5, 2),
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tracking_ride (ride_request_id, recorded_at)
) ENGINE=InnoDB;

-- 4. Notes post-course
CREATE TABLE ride_ratings (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    ride_request_id VARCHAR(255) NOT NULL,
    from_user_id VARCHAR(255) NOT NULL,
    to_user_id VARCHAR(255) NOT NULL,
    rating TINYINT NOT NULL,
    comment VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (ride_request_id, from_user_id),
    CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;

-- 5. Configuration tarifaire
CREATE TABLE pricing_config (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    currency ENUM('FBU', 'USD', 'EUR') NOT NULL DEFAULT 'FBU',
    base_price DECIMAL(10, 2) NOT NULL,
    price_per_km DECIMAL(10, 2) NOT NULL,
    price_per_min DECIMAL(10, 2) NOT NULL,
    minimum_price DECIMAL(10, 2) NOT NULL,
    surge_multiplier DECIMAL(3, 2) NOT NULL DEFAULT 1.00,
    surge_threshold INT NOT NULL DEFAULT 5,
    platform_fee_percent DECIMAL(5, 2) NOT NULL DEFAULT 15.00,
    free_cancellation_minutes INT NOT NULL DEFAULT 2,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO pricing_config (id, name, currency, base_price, price_per_km, price_per_min, minimum_price) VALUES
('default-fbu', 'Tarif par defaut FBU', 'FBU', 1000, 350, 50, 1500),
('default-usd', 'Tarif par defaut USD', 'USD', 1.00, 0.35, 0.05, 1.50),
('default-eur', 'Tarif par defaut EUR', 'EUR', 0.90, 0.30, 0.04, 1.35);

-- 6. Configuration SMS
CREATE TABLE sms_config (
    id VARCHAR(255) NOT NULL PRIMARY KEY,
    provider ENUM('TWILIO', 'AFRICAS_TALKING', 'DISABLED') NOT NULL DEFAULT 'DISABLED',
    enabled TINYINT(1) NOT NULL DEFAULT 0,
    api_key VARCHAR(255),
    api_secret VARCHAR(255),
    sender_number VARCHAR(50),
    sender_name VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO sms_config (id, provider, enabled) VALUES ('default', 'DISABLED', 0);

-- 7. Ajout colonnes sur users
ALTER TABLE users
    ADD COLUMN is_verified_driver TINYINT(1) NOT NULL DEFAULT 0 AFTER identity_verified,
    ADD COLUMN driver_license_url VARCHAR(500) AFTER is_verified_driver;
