-- Run once as a MySQL administrator:  mysql -u root -p < scripts/mysql-setup.sql
-- Replace the password placeholder BEFORE running. Do not commit the edited file.

CREATE DATABASE IF NOT EXISTS attendance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'attendance_app'@'localhost' IDENTIFIED BY 'CHANGE_ME_db_password';

-- Least privilege: only this schema. DDL rights are required because Flyway applies migrations at start-up.
-- (For production, use a separate migration account and give the runtime account DML only.)
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, DROP, INDEX, REFERENCES
    ON attendance_db.* TO 'attendance_app'@'localhost';

FLUSH PRIVILEGES;
