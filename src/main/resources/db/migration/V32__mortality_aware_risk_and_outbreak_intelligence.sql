-- V32: Mortality-Aware Risk & Outbreak Intelligence
-- Adds mortality count tracking columns to outbreaks table and optimizes spatial-temporal queries

ALTER TABLE outbreaks
    ADD COLUMN IF NOT EXISTS mortality_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS vet_confirmed_mortality_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS farmer_reported_mortality_count INTEGER NOT NULL DEFAULT 0;

-- Performance index on outbreak mortality columns
CREATE INDEX IF NOT EXISTS idx_outbreaks_mortality 
    ON outbreaks(mortality_count, vet_confirmed_mortality_count);

-- Performance indexes on animal mortality events for spatial-temporal epidemiology
CREATE INDEX IF NOT EXISTS idx_mortality_events_disease 
    ON animal_mortality_events(disease_name, status, source);

CREATE INDEX IF NOT EXISTS idx_mortality_events_vet_disease 
    ON animal_mortality_events(vet_disease_name, status, source);

CREATE INDEX IF NOT EXISTS idx_mortality_events_lat_lon 
    ON animal_mortality_events(latitude, longitude);

CREATE INDEX IF NOT EXISTS idx_mortality_events_reported_at 
    ON animal_mortality_events(reported_at);
