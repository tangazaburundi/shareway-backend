CREATE TABLE referrals (
    id               VARCHAR(255) NOT NULL DEFAULT (UUID()),
    referrer_id      VARCHAR(255) NOT NULL,
    referral_code    VARCHAR(20)  NOT NULL UNIQUE,
    referred_email   VARCHAR(255),
    referred_user_id VARCHAR(255),
    status           ENUM('PENDING','COMPLETED','EXPIRED') NOT NULL DEFAULT 'PENDING',
    reward_amount    DECIMAL(10,2),
    reward_currency  VARCHAR(10),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     DATETIME,
    PRIMARY KEY (id),
    FOREIGN KEY (referrer_id) REFERENCES users(id),
    INDEX idx_referrals_referrer (referrer_id),
    INDEX idx_referrals_code (referral_code),
    INDEX idx_referrals_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
