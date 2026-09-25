-- Para-vet triage of AI scans: a para-vet can escalate a scan to a vet or close it.
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS review_notes TEXT;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS triage_notes TEXT;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS triaged_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ai_scans ADD COLUMN IF NOT EXISTS triaged_by UUID REFERENCES users(id);

CREATE TABLE IF NOT EXISTS para_vet_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    district VARCHAR(100),
    taluka VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Officer actions on derived operational alerts (alerts themselves are computed, not stored).
CREATE TABLE IF NOT EXISTS alert_actions (
    id UUID PRIMARY KEY,
    alert_id UUID NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL,
    actor_user_id UUID REFERENCES users(id),
    note TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- Production already has this column; fresh databases were missing it and failed validation.
ALTER TABLE mortality_review_audits
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now();
