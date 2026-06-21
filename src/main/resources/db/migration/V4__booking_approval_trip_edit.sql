-- ============================================================
-- V7 : Approbation conducteur + modification trajet
-- ============================================================

-- Colonnes déjà ajoutées dans le modèle Booking (driver_response_at, driver_reject_reason)
-- On s'assure qu'elles existent (idempotent via IF NOT EXISTS n'existe pas en MySQL 5.7,
-- donc on utilise une procédure)
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'bookings'
      AND COLUMN_NAME  = 'driver_response_at'
);
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE bookings ADD COLUMN driver_response_at DATETIME NULL, ADD COLUMN driver_reject_reason TEXT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Enum REJECTED dans bookings.status (MySQL ENUM est extensible via MODIFY)
ALTER TABLE bookings
    MODIFY COLUMN status ENUM('PENDING','CONFIRMED','REJECTED','CANCELLED','COMPLETED')
    NOT NULL DEFAULT 'PENDING';

-- Historique des modifications de trajet
CREATE TABLE IF NOT EXISTS trip_edit_history (
    id           CHAR(36)     NOT NULL DEFAULT (UUID()),
    trip_id      CHAR(36)     NOT NULL,
    edited_by    CHAR(36)     NOT NULL,
    field_changed VARCHAR(100) NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    INDEX idx_trip_edit_history_trip (trip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
