CREATE TABLE IF NOT EXISTS fuel_entries (
    id VARCHAR(36) NOT NULL PRIMARY KEY DEFAULT (UUID()),
    driver_id VARCHAR(36) NOT NULL,
    refuel_date DATE NOT NULL,
    liters DECIMAL(8,3) NOT NULL,
    price_per_liter DECIMAL(10,2) NOT NULL,
    total_cost DECIMAL(10,2) GENERATED ALWAYS AS (liters * price_per_liter) STORED,
    odometer_km DECIMAL(10,1) NULL,
    station_name VARCHAR(255) NULL,
    notes VARCHAR(500) NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'FBU',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
