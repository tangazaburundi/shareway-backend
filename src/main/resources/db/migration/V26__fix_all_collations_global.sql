-- Fix collation on tables involved in ride_ratings JOIN errors
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ride_requests CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
ALTER TABLE ride_ratings CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
SET FOREIGN_KEY_CHECKS = 1;
