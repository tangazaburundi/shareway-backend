-- ============================================================
-- V6 : Historique des modifications de trajet
-- ============================================================

CREATE TABLE IF NOT EXISTS trip_edit_history (
    id            VARCHAR(255)     NOT NULL DEFAULT (UUID()),
    trip_id       VARCHAR(255) NOT NULL,
    edited_by     VARCHAR(255) NOT NULL,
    field_changed VARCHAR(100) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (trip_id) REFERENCES trips(id) ON DELETE CASCADE,
    FOREIGN KEY (edited_by) REFERENCES users(id),
    INDEX idx_trip_edit_history_trip    (trip_id),
    INDEX idx_trip_edit_history_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Colonnes de notification (MySQL ne supporte pas IF NOT EXISTS)
-- ============================================================

ALTER TABLE trip_edit_history
    ADD COLUMN notification_sent TINYINT(1) NOT NULL DEFAULT 0;

ALTER TABLE trip_edit_history
    ADD COLUMN passengers_notified INT NOT NULL DEFAULT 0;