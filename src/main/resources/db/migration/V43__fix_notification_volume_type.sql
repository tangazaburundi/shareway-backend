-- V43: Fix notification_volume back to DECIMAL(3,2) after V41 accidentally changed it to INT
ALTER TABLE `user_sound_preferences`
  MODIFY COLUMN `notification_volume` DECIMAL(3,2) DEFAULT NULL;
