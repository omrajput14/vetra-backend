-- Flyway Migration V9: Create AI Scan Results table for preserving full inference history
CREATE TABLE IF NOT EXISTS ai_scan_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scan_id UUID NOT NULL REFERENCES ai_scans(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64),
    diagnosis TEXT,
    confidence NUMERIC(4,3) CHECK (confidence >= 0.000 AND confidence <= 1.000),
    raw_response TEXT,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    request_id VARCHAR(64),
    tokens_used INT,
    warnings TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance and audit queries
CREATE INDEX IF NOT EXISTS idx_ai_scan_results_scan_id ON ai_scan_results (scan_id);
CREATE INDEX IF NOT EXISTS idx_ai_scan_results_provider ON ai_scan_results (provider);
CREATE INDEX IF NOT EXISTS idx_ai_scan_results_created_at ON ai_scan_results (created_at DESC);
