-- ─────────────────────────────────────────────────────────────────────
-- V24__add_profile_photo_certificates_and_vaccine_attachments.sql
-- Adds profile photo, qualification certificates, clinic address, and vaccination document attachments
-- ─────────────────────────────────────────────────────────────────────

-- 1. Vet Profile Enhancements
ALTER TABLE vet_profiles
    ADD COLUMN IF NOT EXISTS profile_photo_url TEXT,
    ADD COLUMN IF NOT EXISTS certificate_url TEXT,
    ADD COLUMN IF NOT EXISTS clinic_address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS certificate_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION';

-- 2. Farmer Profile Photo
ALTER TABLE farmer_profiles
    ADD COLUMN IF NOT EXISTS profile_photo_url TEXT;

-- 3. Animal Health Records Attachments & Vaccination Tracking
ALTER TABLE animal_health_records
    ADD COLUMN IF NOT EXISTS document_url TEXT,
    ADD COLUMN IF NOT EXISTS vaccine_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS next_due_date DATE,
    ADD COLUMN IF NOT EXISTS batch_number VARCHAR(100);
