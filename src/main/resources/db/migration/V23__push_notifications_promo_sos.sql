-- V23: Push notifications + Promo codes + SOS
-- Push notification device tokens
CREATE TABLE IF NOT EXISTS device_tokens (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'WEB',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_device_token (user_id, token(100)),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Promo codes
CREATE TABLE IF NOT EXISTS promo_codes (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT') NOT NULL DEFAULT 'PERCENTAGE',
    discount_value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2),
    max_discount_amount DECIMAL(10,2),
    max_uses INT NOT NULL DEFAULT 100,
    current_uses INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_from DATETIME NOT NULL,
    valid_until DATETIME NOT NULL,
    currency VARCHAR(10) DEFAULT 'FBU',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Add ride_id column to messages table (idempotent via procedure)
DROP PROCEDURE IF EXISTS _migrate_v23;
DELIMITER //
CREATE PROCEDURE _migrate_v23()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
        AND table_name = 'messages'
        AND column_name = 'ride_id'
    ) THEN
        ALTER TABLE messages ADD COLUMN ride_id VARCHAR(36) DEFAULT NULL;
        CREATE INDEX idx_messages_ride_id ON messages(ride_id);
    END IF;
END //
DELIMITER ;
CALL _migrate_v23();
DROP PROCEDURE IF EXISTS _migrate_v23;

-- Insert sample promo codes
INSERT IGNORE INTO promo_codes (id, code, description, discount_type, discount_value, min_order_amount, max_uses, valid_from, valid_until, currency) VALUES
('PROMO001', 'BIENVENUE10', '10% de reduction sur votre premiere course', 'PERCENTAGE', 10.00, 5000.00, 1000, NOW(), DATE_ADD(NOW(), INTERVAL 6 MONTH), 'FBU'),
('PROMO002', 'SHAREWAY2025', '2000 FBU de reduction', 'FIXED_AMOUNT', 2000.00, 10000.00, 500, NOW(), DATE_ADD(NOW(), INTERVAL 3 MONTH), 'FBU'),
('PROMO003', 'TAXI50', '50% de reduction (max 5000 FBU)', 'PERCENTAGE', 50.00, 3000.00, 200, NOW(), DATE_ADD(NOW(), INTERVAL 1 MONTH), 'FBU');
