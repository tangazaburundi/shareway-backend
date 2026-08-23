CREATE TABLE IF NOT EXISTS user_sound_preferences (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    ride_request_sound VARCHAR(50) NOT NULL DEFAULT 'classic',
    ride_accepted_sound VARCHAR(50) NOT NULL DEFAULT 'success',
    ride_cancelled_sound VARCHAR(50) NOT NULL DEFAULT 'alert',
    message_sound VARCHAR(50) NOT NULL DEFAULT 'ping',
    sos_sound VARCHAR(50) NOT NULL DEFAULT 'siren',
    notification_volume DECIMAL(3,2) NOT NULL DEFAULT 0.30,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    CONSTRAINT fk_sound_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_sound_pref_user UNIQUE (user_id)
);

INSERT IGNORE INTO system_settings (setting_key, setting_value, description) VALUES
('ride.user_sound_config_enabled', 'true', 'Activer la configuration des sons par utilisateur');
