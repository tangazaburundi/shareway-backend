-- 1. Ajouter REFUSED à l'ENUM payment_status de ride_requests
ALTER TABLE ride_requests MODIFY COLUMN payment_status
    ENUM('PENDING','AUTHORIZED','CAPTURED','REFUNDED','FAILED','REFUSED') DEFAULT 'PENDING';

-- 2. Ajouter les colonnes dette sur users
ALTER TABLE users ADD COLUMN total_debt DECIMAL(12,2) DEFAULT 0;
ALTER TABLE users ADD COLUMN debt_currency VARCHAR(10) NULL;
