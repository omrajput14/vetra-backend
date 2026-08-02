-- ─────────────────────────────────────────────────────────────────────
-- V11__create_disease_surveillance.sql
-- Disease Surveillance & Outbreak Detection Foundation Schema
-- Evolves V2 legacy disease_reports and outbreaks into full surveillance schema
-- ─────────────────────────────────────────────────────────────────────

-- 1. Ensure PostGIS Extension is Available
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Evolve V2 disease_reports table: add missing columns
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS animal_id UUID;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS medical_record_id UUID;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS ai_scan_id UUID;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS reported_by UUID;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS report_source VARCHAR(32) NOT NULL DEFAULT 'VETERINARIAN';
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS disease_name VARCHAR(128) NOT NULL DEFAULT 'Unknown';
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS diagnosis_status VARCHAR(32) NOT NULL DEFAULT 'SUSPECTED';
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE disease_reports ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 3. Evolve V2 outbreaks table: add missing columns
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS center_latitude DOUBLE PRECISION;
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS center_longitude DOUBLE PRECISION;
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS radius_km DOUBLE PRECISION NOT NULL DEFAULT 10.00;
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS affected_reports_count INT NOT NULL DEFAULT 0;
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE outbreaks ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- 4. Disease Reports Table (for fresh databases without V2)
CREATE TABLE IF NOT EXISTS disease_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    medical_record_id UUID REFERENCES medical_records(id) ON DELETE SET NULL,
    ai_scan_id UUID REFERENCES ai_scans(id) ON DELETE SET NULL,
    reported_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_source VARCHAR(32) NOT NULL,
    disease_name VARCHAR(128) NOT NULL,
    diagnosis_status VARCHAR(32) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location GEOMETRY(Point, 4326),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_disease_report_source CHECK (report_source IN ('AI_VERIFIED', 'VETERINARIAN', 'LAB_RESULT', 'MANUAL')),
    CONSTRAINT chk_diagnosis_status CHECK (diagnosis_status IN ('SUSPECTED', 'CONFIRMED', 'REJECTED'))
);

-- 5. Outbreaks Table (for fresh databases without V2)
CREATE TABLE IF NOT EXISTS outbreaks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    disease_name VARCHAR(128) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    center_latitude DOUBLE PRECISION NOT NULL,
    center_longitude DOUBLE PRECISION NOT NULL,
    center_location GEOMETRY(Point, 4326),
    radius_km DOUBLE PRECISION NOT NULL DEFAULT 10.00,
    affected_reports_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_outbreak_status CHECK (status IN ('ACTIVE', 'MONITORING', 'RESOLVED'))
);

-- 6. Spatial GiST Indexes
CREATE INDEX IF NOT EXISTS idx_disease_reports_location ON disease_reports USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_outbreaks_center_location ON outbreaks USING GIST (center_location);

-- 7. Standard B-Tree Indexes
CREATE INDEX IF NOT EXISTS idx_disease_reports_animal ON disease_reports(animal_id);
CREATE INDEX IF NOT EXISTS idx_disease_reports_reported_by ON disease_reports(reported_by);
CREATE INDEX IF NOT EXISTS idx_disease_reports_disease ON disease_reports(disease_name);
CREATE INDEX IF NOT EXISTS idx_disease_reports_status ON disease_reports(diagnosis_status);
CREATE INDEX IF NOT EXISTS idx_outbreaks_disease ON outbreaks(disease_name);
CREATE INDEX IF NOT EXISTS idx_outbreaks_status ON outbreaks(status);
