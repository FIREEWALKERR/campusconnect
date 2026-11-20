-- Run as a MySQL administrator. Replace the example password before execution.
CREATE DATABASE IF NOT EXISTS campusconnect
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'campusconnect'@'localhost'
  IDENTIFIED BY 'REPLACE_WITH_YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON campusconnect.* TO 'campusconnect'@'localhost';
-- Hibernate creates the tables from the annotated entity model on first run.
-- No global database privileges are needed by the application account.
