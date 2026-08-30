-- ─────────────────────────────────────────────────────────────────────
-- V23__add_vet_shift_schedule.sql
-- Add shift schedule JSON/text support to vet profiles
-- ─────────────────────────────────────────────────────────────────────

ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS shift_schedule TEXT;
