-- V41: Fix collation column level on user_sound_preferences
-- user_id VARCHAR(255)*4=1020 > 1000 byte index limit → shrink to VARCHAR(191)
-- Index name from V33: uk_sound_pref_user
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `user_sound_preferences` DROP INDEX `uk_sound_pref_user`;

ALTER TABLE `user_sound_preferences`
  MODIFY COLUMN `id` VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  MODIFY COLUMN `user_id` VARCHAR(191) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  MODIFY COLUMN `message_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `notification_volume` DECIMAL(3,2) DEFAULT NULL,
  MODIFY COLUMN `ride_accepted_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `ride_cancelled_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `ride_completed_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `ride_rendered_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `ride_request_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `sos_sound` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  MODIFY COLUMN `created_at` DATETIME DEFAULT NULL,
  MODIFY COLUMN `updated_at` DATETIME DEFAULT NULL;

ALTER TABLE `user_sound_preferences`
  ADD CONSTRAINT `uk_sound_pref_user` UNIQUE (`user_id`);

SET FOREIGN_KEY_CHECKS = 1;
