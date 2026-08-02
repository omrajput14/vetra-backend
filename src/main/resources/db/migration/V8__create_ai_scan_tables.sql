-- ─────────────────────────────────────────────────────────────────────
-- V8__create_ai_scan_tables.sql
-- AI Diagnostic Scans schema evolution
-- Evolves the V2 legacy ai_scans table into the full diagnostic schema
-- ─────────────────────────────────────────────────────────────────────

-- 1. Safely add missing columns if table already existed from V2 schema
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS animal_id UUID;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS uploaded_by UUID;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS image_hash VARCHAR(64);
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS ai_provider VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS ai_model VARCHAR(64);
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS diagnosis TEXT;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'PENDING';
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS veterinarian_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS verified_by UUID;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- 2. Fix confidence_score type (V2 used DOUBLE PRECISION, entity expects NUMERIC(4,3))
ALTER TABLE ai_scans ALTER COLUMN confidence_score TYPE NUMERIC(4,3)
    USING confidence_score::NUMERIC(4,3);

-- 3. Create table if not exists (for fresh databases without V2)
CREATE TABLE IF NOT EXISTS ai_scans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    uploaded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    image_url VARCHAR(512) NOT NULL,
    image_hash VARCHAR(64),
    ai_provider VARCHAR(32) NOT NULL DEFAULT 'NONE',
    ai_model VARCHAR(64),
    diagnosis TEXT,
    confidence_score NUMERIC(4,3) CHECK (confidence_score >= 0.000 AND confidence_score <= 1.000),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    veterinarian_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by UUID REFERENCES users(id) ON DELETE SET NULL,
    verified_at TIMESTAMPTZ,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Indexes for optimal lookup performance
CREATE INDEX IF NOT EXISTS idx_ai_scans_animal_id ON ai_scans (animal_id);
CREATE INDEX IF NOT EXISTS idx_ai_scans_uploaded_by ON ai_scans (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_ai_scans_status ON ai_scans (status);
CREATE INDEX IF NOT EXISTS idx_ai_scans_created_at ON ai_scans (created_at DESC);
