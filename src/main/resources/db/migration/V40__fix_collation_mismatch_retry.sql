-- V40: Fix collation mismatch between user_sound_preferences and users tables
-- The previous V39 failed, so this is a fresh attempt
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `user_sound_preferences` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
