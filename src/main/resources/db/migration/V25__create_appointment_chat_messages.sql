-- ─────────────────────────────────────────────────────────────────────
-- V25__create_appointment_chat_messages.sql
-- Direct farmer-vet consultation chat and structured treatment instructions
-- ─────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS appointment_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    content TEXT NOT NULL,
    treatment_payload_json TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_appointment_id ON appointment_chat_messages(appointment_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created_at ON appointment_chat_messages(created_at ASC);
