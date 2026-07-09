CREATE TABLE IF NOT EXISTS penalties (
    id           VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id      VARCHAR(255) NOT NULL,
    booking_id   VARCHAR(255),
    trip_id      VARCHAR(255),
    type         ENUM('LATE_CANCELLATION','NO_SHOW','DRIVER_NO_SHOW') NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    currency     VARCHAR(10) NOT NULL,
    reason       TEXT,
    is_paid      TINYINT(1) NOT NULL DEFAULT 0,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at      DATETIME,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_penalties_user (user_id),
    INDEX idx_penalties_booking (booking_id),
    INDEX idx_penalties_trip (trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
