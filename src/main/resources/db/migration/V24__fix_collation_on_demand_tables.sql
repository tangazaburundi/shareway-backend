-- V24: Harmoniser les collations des tables on-demand avec users
-- Erreur: Illegal mix of collations (utf8mb4_unicode_ci) vs (utf8mb4_0900_ai_ci)
-- La table users utilise utf8mb4_0900_ai_ci, les tables V22 ont hérité du défaut (unicode_ci)

ALTER TABLE driver_availability CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ride_requests CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ride_tracking CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ride_ratings CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE pricing_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE sms_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
