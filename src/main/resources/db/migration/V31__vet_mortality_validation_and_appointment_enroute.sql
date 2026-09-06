-- ─────────────────────────────────────────────────────────────────────────────
-- V31__vet_mortality_validation_and_appointment_enroute.sql
-- SIH26128 Phase 2: Veterinarian Mortality Validation, Audit Trail, Referrals,
-- and Appointment EN_ROUTE Live Tracking
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Extend animal_mortality_events with veterinarian review metadata
ALTER TABLE animal_mortality_events
    ADD COLUMN IF NOT EXISTS vet_reviewed_by UUID REFERENCES vet_profiles(id),
    ADD COLUMN IF NOT EXISTS vet_reviewed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS vet_cause_category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS vet_disease_name VARCHAR(128),
    ADD COLUMN IF NOT EXISTS vet_clinical_notes TEXT,
    ADD COLUMN IF NOT EXISTS vet_rejection_reason TEXT,
    ADD COLUMN IF NOT EXISTS post_mortem_conducted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_mortality_vet_reviewed_by
    ON animal_mortality_events(vet_reviewed_by);

-- 2. Create immutable mortality review audit trail table
CREATE TABLE IF NOT EXISTS mortality_review_audits (
    id UUID PRIMARY KEY,
    mortality_event_id UUID NOT NULL REFERENCES animal_mortality_events(id) ON DELETE CASCADE,
    veterinarian_id UUID NOT NULL REFERENCES vet_profiles(id),
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    clinical_notes TEXT,
    confirmed_cause_category VARCHAR(50),
    confirmed_disease_name VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mortality_audits_event_id
    ON mortality_review_audits(mortality_event_id);

CREATE INDEX IF NOT EXISTS idx_mortality_audits_vet_id
    ON mortality_review_audits(veterinarian_id);

CREATE INDEX IF NOT EXISTS idx_mortality_audits_created_at
    ON mortality_review_audits(created_at);

-- 3. Create mortality referrals table for targeted nearby vet case inbox
CREATE TABLE IF NOT EXISTS mortality_referrals (
    id UUID PRIMARY KEY,
    mortality_event_id UUID NOT NULL REFERENCES animal_mortality_events(id) ON DELETE CASCADE,
    veterinarian_id UUID NOT NULL REFERENCES vet_profiles(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    distance_km DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_mortality_referral UNIQUE (mortality_event_id, veterinarian_id)
);

CREATE INDEX IF NOT EXISTS idx_mortality_referrals_vet_status
    ON mortality_referrals(veterinarian_id, status);

CREATE INDEX IF NOT EXISTS idx_mortality_referrals_event_id
    ON mortality_referrals(mortality_event_id);

-- 4. Extend appointments table with live tracking coordinates during EN_ROUTE
ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS vet_latitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS vet_longitude DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS vet_location_updated_at TIMESTAMP WITH TIME ZONE;
