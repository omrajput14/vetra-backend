-- ─────────────────────────────────────────────────────────────────────
-- V30__create_animal_mortality_events.sql
-- Mortality Data Foundation & Farmer Mortality Reporting
-- ─────────────────────────────────────────────────────────────────────

-- 1. Extend animals table with status lifecycle tracking
ALTER TABLE animals ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_animals_status') THEN
        ALTER TABLE animals ADD CONSTRAINT chk_animals_status CHECK (status IN ('ACTIVE', 'DECEASED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_animals_status ON animals(status);

-- 2. Animal Mortality Events table
CREATE TABLE IF NOT EXISTS animal_mortality_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE RESTRICT,
    farmer_id UUID NOT NULL REFERENCES farmer_profiles(id) ON DELETE RESTRICT,
    reported_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    tag_number VARCHAR(100) NOT NULL,
    qr_code_id VARCHAR(100),
    cause_category VARCHAR(50) NOT NULL,
    cause_description TEXT,
    disease_name VARCHAR(128),
    recently_treated BOOLEAN NOT NULL DEFAULT FALSE,
    treatment_notes TEXT,
    notes TEXT,
    source VARCHAR(30) NOT NULL DEFAULT 'FARMER_REPORTED',
    status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
    death_date_time TIMESTAMP WITH TIME ZONE,
    reported_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location_accuracy DOUBLE PRECISION,
    location GEOMETRY(Point, 4326),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_mortality_cause_category CHECK (cause_category IN ('KNOWN_DISEASE', 'SUSPECTED_DISEASE', 'ACCIDENT_INJURY', 'UNKNOWN', 'OTHER')),
    CONSTRAINT chk_mortality_source CHECK (source IN ('FARMER_REPORTED', 'VET_CONFIRMED')),
    CONSTRAINT chk_mortality_status CHECK (status IN ('REPORTED', 'PENDING_REVIEW', 'CONFIRMED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_mortality_animal_id ON animal_mortality_events(animal_id);
CREATE INDEX IF NOT EXISTS idx_mortality_farmer_id ON animal_mortality_events(farmer_id);
CREATE INDEX IF NOT EXISTS idx_mortality_reported_at ON animal_mortality_events(reported_at DESC);
CREATE INDEX IF NOT EXISTS idx_mortality_status ON animal_mortality_events(status);
CREATE INDEX IF NOT EXISTS idx_mortality_cause_category ON animal_mortality_events(cause_category);
CREATE INDEX IF NOT EXISTS idx_mortality_location ON animal_mortality_events USING GIST(location);
