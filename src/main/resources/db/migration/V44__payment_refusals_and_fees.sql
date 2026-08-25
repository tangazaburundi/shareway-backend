-- V44: Payment refusals + admin fee/fine settings
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS payment_refusals (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    ride_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(191) NOT NULL,
    user_first_name VARCHAR(100),
    user_last_name VARCHAR(100),
    pickup_address TEXT,
    destination_address TEXT,
    estimated_distance_km DECIMAL(10,2),
    original_amount DECIMAL(12,2) NOT NULL,
    fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    fine_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_debt DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'FBU',
    resolved TINYINT(1) NOT NULL DEFAULT 0,
    resolved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_refusals_user (user_id),
    INDEX idx_payment_refusals_ride (ride_id)
);

SET FOREIGN_KEY_CHECKS = 1;

INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('ride.unpaid_fee_percent', '10', 'Frais de dossier en % sur le montant non payé'),
('ride.unpaid_fine_amount', '5000', 'Amende fixe en devise locale pour refus de paiement');
