-- ─────────────────────────────────────────────────────────────────────
-- V36__add_clinical_link_to_animal_health_records.sql
-- Adds deterministic clinical linkage (medical_record_id, appointment_id)
-- ─────────────────────────────────────────────────────────────────────

ALTER TABLE animal_health_records
    ADD COLUMN IF NOT EXISTS medical_record_id UUID REFERENCES medical_records(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS appointment_id UUID REFERENCES appointments(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_health_records_medical_record_id ON animal_health_records(medical_record_id);
CREATE INDEX IF NOT EXISTS idx_health_records_appointment_id ON animal_health_records(appointment_id);
