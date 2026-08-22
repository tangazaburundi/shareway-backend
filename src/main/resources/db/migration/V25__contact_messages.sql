CREATE TABLE IF NOT EXISTS contact_messages (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    sujet VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    email_sent TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_contact_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
