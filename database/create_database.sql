-- MySQL 8+. Replace the sample password before executing.
CREATE DATABASE IF NOT EXISTS runway_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'runway'@'localhost' IDENTIFIED BY 'CHANGE_THIS_PASSWORD';
GRANT ALL PRIVILEGES ON runway_store.* TO 'runway'@'localhost';
-- Set DB_USERNAME=runway and DB_PASSWORD to the password chosen above.
-- Flyway creates tables in a fresh schema. Production seed is disabled.
-- Before migrating existing data, follow docs/DEPLOYMENT.md; never auto-baseline blindly.
