-- ============================================================
-- V5 : Sécurité authentification admin
-- ============================================================

ALTER TABLE users
ADD COLUMN system_role ENUM('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'SUPPORT') NULL DEFAULT NULL AFTER role;

ALTER TABLE users
ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;

ALTER TABLE users
ADD COLUMN locked_until DATETIME NULL;

CREATE TABLE IF NOT EXISTS admin_login_audit (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id     VARCHAR(255),
    email       VARCHAR(255) NOT NULL,
    success     TINYINT(1)   NOT NULL,
    reason      VARCHAR(255),
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_admin_login_audit_email (email),
    INDEX idx_admin_login_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin_refresh_tokens (
    id          CHAR(36)     NOT NULL DEFAULT (UUID()),
    user_id     VARCHAR(255) NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  DATETIME     NOT NULL,
    revoked     TINYINT(1)   NOT NULL DEFAULT 0,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admin_refresh_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;