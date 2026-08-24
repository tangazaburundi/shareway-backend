-- Désactiver FK checks pour éviter l'erreur de collation
SET FOREIGN_KEY_CHECKS = 0;

-- Ajouter blocked_until aux users (cooldown après render/cancel)
ALTER TABLE users ADD COLUMN blocked_until DATETIME NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- Paramètre admin : durée du cooldown en minutes (défaut 15)
INSERT IGNORE INTO system_settings (setting_key, setting_value, description)
VALUES ('ride.driver_cooldown_minutes', '15', 'Durée en minutes pendant laquelle un chauffeur ne peut pas se remettre en ligne après avoir rendu ou annulé une course acceptée');
