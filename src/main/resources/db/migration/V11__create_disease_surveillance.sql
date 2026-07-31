-- ─────────────────────────────────────────────────────────────────────
-- V11__create_disease_surveillance.sql
-- Disease Surveillance & Outbreak Detection Foundation Schema
-- ─────────────────────────────────────────────────────────────────────

-- 1. Ensure PostGIS Extension is Available
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. Disease Reports Table
CREATE TABLE IF NOT EXISTS disease_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    medical_record_id UUID REFERENCES medical_records(id) ON DELETE SET NULL,
    ai_scan_id UUID REFERENCES ai_scans(id) ON DELETE SET NULL,
    reported_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    report_source VARCHAR(32) NOT NULL,
    disease_name VARCHAR(128) NOT NULL,
    diagnosis_status VARCHAR(32) NOT NULL,
    latitude NUMERIC(9,6) NOT NULL,
    longitude NUMERIC(9,6) NOT NULL,
    location GEOMETRY(Point, 4326),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_disease_report_source CHECK (report_source IN ('AI_VERIFIED', 'VETERINARIAN', 'LAB_RESULT', 'MANUAL')),
    CONSTRAINT chk_diagnosis_status CHECK (diagnosis_status IN ('SUSPECTED', 'CONFIRMED', 'REJECTED'))
);

-- 3. Outbreaks Table
CREATE TABLE IF NOT EXISTS outbreaks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    disease_name VARCHAR(128) NOT NULL,
    severity VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    center_latitude NUMERIC(9,6) NOT NULL,
    center_longitude NUMERIC(9,6) NOT NULL,
    center_location GEOMETRY(Point, 4326),
    radius_km NUMERIC(8,2) NOT NULL DEFAULT 10.00,
    affected_reports_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_outbreak_status CHECK (status IN ('ACTIVE', 'MONITORING', 'RESOLVED'))
);

-- 4. Spatial GiST Indexes
CREATE INDEX IF NOT EXISTS idx_disease_reports_location ON disease_reports USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_outbreaks_center_location ON outbreaks USING GIST (center_location);

-- 5. Standard B-Tree Indexes
CREATE INDEX IF NOT EXISTS idx_disease_reports_animal ON disease_reports(animal_id);
CREATE INDEX IF NOT EXISTS idx_disease_reports_reported_by ON disease_reports(reported_by);
CREATE INDEX IF NOT EXISTS idx_disease_reports_disease ON disease_reports(disease_name);
CREATE INDEX IF NOT EXISTS idx_disease_reports_status ON disease_reports(diagnosis_status);
CREATE INDEX IF NOT EXISTS idx_outbreaks_disease ON outbreaks(disease_name);
CREATE INDEX IF NOT EXISTS idx_outbreaks_status ON outbreaks(status);
