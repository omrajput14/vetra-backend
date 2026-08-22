-- ─────────────────────────────────────────────────────────────────────
-- V19__add_vet_emergency_availability.sql
-- Add emergency availability support to vet profiles
-- ─────────────────────────────────────────────────────────────────────

ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS emergency_available BOOLEAN NOT NULL DEFAULT TRUE;
