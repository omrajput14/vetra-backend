-- ─────────────────────────────────────────────────────────────────────
-- V33__create_vaccination_campaigns.sql
-- Phase 4A: Government Response + Vaccination Campaign Management
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS vaccination_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_name VARCHAR(150) NOT NULL,
    disease_name VARCHAR(128) NOT NULL,
    target_district VARCHAR(100) NOT NULL,
    target_taluka VARCHAR(100),
    target_livestock_count INTEGER NOT NULL DEFAULT 0,
    planned_doses INTEGER NOT NULL,
    administered_doses INTEGER NOT NULL DEFAULT 0,
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    start_date DATE NOT NULL,
    end_date DATE,
    outbreak_id UUID REFERENCES outbreaks(id) ON DELETE SET NULL,
    created_by_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_campaign_planned_doses CHECK (planned_doses > 0),
    CONSTRAINT chk_campaign_administered_doses CHECK (administered_doses >= 0),
    CONSTRAINT chk_campaign_target_livestock CHECK (target_livestock_count >= 0),
    CONSTRAINT chk_campaign_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_campaign_status CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_campaigns_status ON vaccination_campaigns(status);
CREATE INDEX IF NOT EXISTS idx_campaigns_district ON vaccination_campaigns(target_district);
CREATE INDEX IF NOT EXISTS idx_campaigns_status_district ON vaccination_campaigns(status, target_district);
CREATE INDEX IF NOT EXISTS idx_campaigns_disease ON vaccination_campaigns(disease_name);
CREATE INDEX IF NOT EXISTS idx_campaigns_outbreak ON vaccination_campaigns(outbreak_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_created_at ON vaccination_campaigns(created_at DESC);

-- Audit log table for tracking officer actions and status changes
CREATE TABLE IF NOT EXISTS vaccination_campaign_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id UUID NOT NULL REFERENCES vaccination_campaigns(id) ON DELETE CASCADE,
    performed_by_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL,
    previous_status VARCHAR(30),
    new_status VARCHAR(30),
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_campaign_audit_action CHECK (action IN ('CREATED', 'STATUS_CHANGED', 'UPDATED'))
);

CREATE INDEX IF NOT EXISTS idx_campaign_audit_campaign ON vaccination_campaign_audit_logs(campaign_id);
CREATE INDEX IF NOT EXISTS idx_campaign_audit_created_at ON vaccination_campaign_audit_logs(created_at DESC);
