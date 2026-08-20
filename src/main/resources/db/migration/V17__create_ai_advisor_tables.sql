-- =============================================================================
-- Migration V17: AI Veterinary Advisor Sessions & Conversation Messages
-- Description: Creates tables for interactive, context-aware veterinary screening sessions.
-- =============================================================================

CREATE TABLE IF NOT EXISTS ai_advisor_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    animal_id UUID NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'QUESTIONING',
    risk_level VARCHAR(50) DEFAULT 'UNKNOWN',
    requires_vet_review BOOLEAN NOT NULL DEFAULT TRUE,
    assessment_json TEXT,
    turn_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_advisor_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES ai_advisor_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(20) NOT NULL, -- 'USER' or 'ADVISOR'
    content TEXT NOT NULL,
    structured_payload TEXT,
    turn_number INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexing for optimized session lookup by animal, user, and conversation ordering
CREATE INDEX IF NOT EXISTS idx_ai_advisor_sessions_animal_id ON ai_advisor_sessions(animal_id);
CREATE INDEX IF NOT EXISTS idx_ai_advisor_sessions_user_id ON ai_advisor_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_advisor_sessions_created_at ON ai_advisor_sessions(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_advisor_sessions_status ON ai_advisor_sessions(status);

CREATE INDEX IF NOT EXISTS idx_ai_advisor_messages_session_id ON ai_advisor_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_advisor_messages_created_at ON ai_advisor_messages(created_at ASC);
