CREATE TABLE IF NOT EXISTS ride_rejections (
    id VARCHAR(191) PRIMARY KEY DEFAULT (UUID()),
    ride_id VARCHAR(191) NOT NULL,
    driver_id VARCHAR(191) NOT NULL,
    passenger_id VARCHAR(191) NULL,
    reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    INDEX idx_rejection_ride (ride_id),
    INDEX idx_rejection_driver (driver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
