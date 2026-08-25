-- V42: Add consecutive_refusals counter to users for progressive penalty
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE `users` ADD COLUMN `consecutive_refusals` INT NOT NULL DEFAULT 0;
SET FOREIGN_KEY_CHECKS = 1;
