-- ─────────────────────────────────────────────────────────────────────
-- V20__add_vet_verification_status.sql
-- Add verification status lifecycle to vet profiles
-- ─────────────────────────────────────────────────────────────────────

ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- Migrate existing registered veterinarians to VERIFIED to ensure testing and demo continuity
UPDATE vet_profiles SET verification_status = 'VERIFIED' WHERE verification_status = 'PENDING';
