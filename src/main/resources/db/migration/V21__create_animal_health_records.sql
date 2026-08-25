-- ─────────────────────────────────────────────────────────────────────
-- V21__create_animal_health_records.sql
-- Animal Lifetime Medical History & Health Timeline
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS animal_health_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    record_type VARCHAR(30) NOT NULL,
    source VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    title VARCHAR(200) NOT NULL,
    description TEXT,
    symptoms TEXT,
    diagnosis TEXT,
    treatment TEXT,
    veterinarian_id UUID,
    veterinarian_name VARCHAR(150),
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_health_records_animal_id ON animal_health_records(animal_id);
CREATE INDEX IF NOT EXISTS idx_health_records_recorded_at ON animal_health_records(recorded_at DESC);
CREATE INDEX IF NOT EXISTS idx_health_records_record_type ON animal_health_records(record_type);
CREATE INDEX IF NOT EXISTS idx_health_records_source ON animal_health_records(source);
