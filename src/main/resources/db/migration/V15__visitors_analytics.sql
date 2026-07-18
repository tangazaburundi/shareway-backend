CREATE TABLE visitors (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NULL,
    user_name VARCHAR(150) NULL,
    user_email VARCHAR(255) NULL,
    anonymous_id VARCHAR(100) NULL,
    ip_address VARCHAR(45) NULL,
    country VARCHAR(100) NULL,
    city VARCHAR(100) NULL,
    page_url VARCHAR(500) NULL,
    referrer VARCHAR(500) NULL,
    user_agent VARCHAR(500) NULL,
    accepted_cookies BOOLEAN DEFAULT FALSE,
    visited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_visited_at (visited_at),
    INDEX idx_user_id (user_id),
    INDEX idx_country (country),
    INDEX idx_anonymous_id (anonymous_id)
);
