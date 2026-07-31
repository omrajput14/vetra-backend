-- ─────────────────────────────────────────────────────────────────────
-- V12__outbreak_intelligence_engine.sql
-- Outbreak Intelligence Engine: Risk Scoring, Confidence Source & Time Window Schema Extensions
-- ─────────────────────────────────────────────────────────────────────

-- 1. Extend disease_reports table with diagnosis confidence source
ALTER TABLE disease_reports
    ADD COLUMN IF NOT EXISTS diagnosis_confidence_source VARCHAR(32) NOT NULL DEFAULT 'VETERINARIAN';

-- 2. Extend outbreaks table with risk score, evaluation window and last reported case timestamp
ALTER TABLE outbreaks
    ADD COLUMN IF NOT EXISTS risk_score VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    ADD COLUMN IF NOT EXISTS evaluation_window_hours INT NOT NULL DEFAULT 72,
    ADD COLUMN IF NOT EXISTS last_case_reported_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 3. Update Outbreak Status constraint to support full lifecycle (DETECTED, ACTIVE, MONITORING, RESOLVED)
ALTER TABLE outbreaks DROP CONSTRAINT IF EXISTS chk_outbreak_status;
ALTER TABLE outbreaks ADD CONSTRAINT chk_outbreak_status
    CHECK (status IN ('DETECTED', 'ACTIVE', 'MONITORING', 'RESOLVED'));

-- 4. B-Tree Indexes for High-Risk Queries and Statistics
CREATE INDEX IF NOT EXISTS idx_outbreaks_risk_score ON outbreaks(risk_score);
CREATE INDEX IF NOT EXISTS idx_outbreaks_last_case_reported ON outbreaks(last_case_reported_at);
