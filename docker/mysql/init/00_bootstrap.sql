-- Bootstrap script for the Smart Watch MySQL container.
-- Runs first (alphabetically) inside /docker-entrypoint-initdb.d/ on a fresh
-- data volume. Ensures the database exists before the schema files execute.

CREATE DATABASE IF NOT EXISTS smart_watch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_watch;
