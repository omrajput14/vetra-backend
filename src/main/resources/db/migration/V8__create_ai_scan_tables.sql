-- Flyway Migration V8: Create AI Diagnostic Scans schema
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

-- Indexes for optimal lookup performance
CREATE INDEX IF NOT EXISTS idx_ai_scans_animal_id ON ai_scans (animal_id);
CREATE INDEX IF NOT EXISTS idx_ai_scans_uploaded_by ON ai_scans (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_ai_scans_status ON ai_scans (status);
CREATE INDEX IF NOT EXISTS idx_ai_scans_created_at ON ai_scans (created_at DESC);
