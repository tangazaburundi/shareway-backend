-- ============================================================
-- SHAREWAY - V12 : Validation admin + demandes de rôle + admin par défaut
-- ============================================================

-- 1. Ajouter admin_approved à la table users
ALTER TABLE users
    ADD COLUMN admin_approved    TINYINT(1)   NOT NULL DEFAULT 0 AFTER identity_verified_by,
    ADD COLUMN admin_approved_at DATETIME     NULL     AFTER admin_approved,
    ADD COLUMN admin_approved_by VARCHAR(255) NULL     AFTER admin_approved_at,
    ADD INDEX idx_users_admin_approved (admin_approved);

-- 2. Créer la table role_requests
CREATE TABLE IF NOT EXISTS role_requests (
    id             VARCHAR(255) NOT NULL DEFAULT (UUID()),
    user_id        VARCHAR(255) NOT NULL,
    requested_role ENUM('DRIVER','PASSENGER','BOTH') NOT NULL,
    status         ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reason         TEXT,
    reviewed_by    VARCHAR(255),
    reviewed_at    DATETIME,
    review_comment TEXT,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_role_requests_user (user_id),
    INDEX idx_role_requests_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Insérer l'admin par défaut (email: sharewaybdi@gmail.com, password: Rurenza2020+)
--    Hash bcrypt généré avec BCryptPasswordEncoder(12)
INSERT INTO users (id, first_name, last_name, email, password_hash, phone, role, preferred_lang,
                   is_active, is_blocked, email_verified, admin_approved, admin_approved_at, created_at)
VALUES (
    UUID(),
    'Admin',
    'ShareWay',
    'sharewaybdi@gmail.com',
    '$2a$12$qXKUEvhU7vArPmQl9nfafedBKhXn4v4cjGYy4eK04KSL8OBNgJBqG',
    NULL,
    'BOTH',
    'fr',
    1, 0, 1, 1, NOW(), NOW()
);

-- 4. Insérer le rôle SUPER_ADMIN pour cet admin
-- On récupère l'ID via une variable
SET @admin_id = (SELECT id FROM users WHERE email = 'sharewaybdi@gmail.com' LIMIT 1);

INSERT INTO admin_roles (id, user_id, role, permissions, granted_by, granted_at)
VALUES (
    UUID(),
    @admin_id,
    'SUPER_ADMIN',
    '{"ALL": true}',
    @admin_id,
    NOW()
);
