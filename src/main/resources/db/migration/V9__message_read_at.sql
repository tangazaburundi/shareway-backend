SET @dbname = DATABASE();
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'messages' AND COLUMN_NAME = 'read_at' AND TABLE_SCHEMA = @dbname);
SET @sql = IF(@col_exists = 0, 'ALTER TABLE messages ADD COLUMN read_at DATETIME NULL AFTER is_read', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;