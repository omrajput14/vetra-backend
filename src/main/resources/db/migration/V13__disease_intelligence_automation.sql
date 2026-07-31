-- ─────────────────────────────────────────────────────────────────────
-- V13__disease_intelligence_automation.sql
-- Autonomous Disease Intelligence Engine: Trend Analysis, Automatic Resolution & Analytics Extensions
-- ─────────────────────────────────────────────────────────────────────

-- 1. Extend outbreaks table with trend analysis, evaluation timestamps and resolution metadata
ALTER TABLE outbreaks
    ADD COLUMN IF NOT EXISTS trend VARCHAR(32) NOT NULL DEFAULT 'STABLE',
    ADD COLUMN IF NOT EXISTS last_evaluated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS resolution_reason VARCHAR(128);

-- 2. Performance Indexes for Outbreak Analytics & Geo-filtering
CREATE INDEX IF NOT EXISTS idx_outbreaks_trend ON outbreaks(trend);
CREATE INDEX IF NOT EXISTS idx_outbreaks_resolved_at ON outbreaks(resolved_at);
