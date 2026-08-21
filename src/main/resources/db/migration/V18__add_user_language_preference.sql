-- Flyway Migration: V18__add_user_language_preference.sql
-- Add preferred language support for multi-language user profiles (en, hi, mr)

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT 'en' NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_preferred_language ON users(preferred_language);
