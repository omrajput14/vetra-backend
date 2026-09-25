-- Para-vets act only after a government officer approves them.
ALTER TABLE para_vet_profiles
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING';

-- A vaccination dose recorded in a campaign drive: which campaign, and who gave it.
ALTER TABLE animal_health_records ADD COLUMN IF NOT EXISTS campaign_id UUID REFERENCES vaccination_campaigns(id);
ALTER TABLE animal_health_records ADD COLUMN IF NOT EXISTS administered_by UUID REFERENCES users(id);
CREATE INDEX IF NOT EXISTS idx_health_records_campaign ON animal_health_records (campaign_id);
