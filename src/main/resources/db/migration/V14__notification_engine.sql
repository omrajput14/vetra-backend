-- ─────────────────────────────────────────────────────────────────────
-- V14__notification_engine.sql
-- Notification & Communication Platform Schema
-- ─────────────────────────────────────────────────────────────────────

-- 1. Notification Templates Table
CREATE TABLE IF NOT EXISTS notification_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    subject_template VARCHAR(256) NOT NULL,
    body_template TEXT NOT NULL,
    default_channel VARCHAR(32) NOT NULL DEFAULT 'PUSH',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- 2. User Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    appointment_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    vaccination_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    ai_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    outbreak_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- 3. Notification Devices Table
CREATE TABLE IF NOT EXISTS notification_devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token VARCHAR(512) NOT NULL UNIQUE,
    platform VARCHAR(32) NOT NULL DEFAULT 'ANDROID',
    app_version VARCHAR(32),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- 4. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_id UUID REFERENCES notification_templates(id) ON DELETE SET NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'PUSH',
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    title VARCHAR(256) NOT NULL,
    body TEXT NOT NULL,
    payload_json TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_notification_channel CHECK (channel IN ('PUSH', 'EMAIL', 'SMS', 'WEBHOOK')),
    CONSTRAINT chk_notification_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'QUEUED', 'SENT', 'DELIVERED', 'READ', 'FAILED'))
);

-- 5. Notification Delivery Log Table
CREATE TABLE IF NOT EXISTS notification_delivery_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    provider VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    response_payload TEXT,
    error_message TEXT,
    attempt_number INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 6. Performance Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_status ON notifications(status);
CREATE INDEX IF NOT EXISTS idx_notification_devices_user ON notification_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_devices_token ON notification_devices(device_token);

-- 7. Seed Initial System Notification Templates
INSERT INTO notification_templates (id, code, name, subject_template, body_template, default_channel)
VALUES
    ('a0000000-0000-0000-0000-000000000001', 'APPOINTMENT_CONFIRMED', 'Appointment Confirmed', 'Appointment Confirmed for {animalName}', 'Your appointment for {animalName} with Dr. {vetName} on {date} is confirmed.', 'PUSH'),
    ('a0000000-0000-0000-0000-000000000002', 'AI_SCAN_COMPLETED', 'AI Diagnostic Scan Completed', 'AI Analysis Complete for {animalName}', 'AI Diagnostic analysis for {animalName} is ready. Confidence score: {confidence}%.', 'PUSH'),
    ('a0000000-0000-0000-0000-000000000003', 'AI_SCAN_VERIFIED', 'AI Diagnosis Verified by Vet', 'Veterinary Verification for {animalName}', 'Dr. {vetName} verified the AI diagnosis for {animalName}. Official EVMR created.', 'PUSH'),
    ('a0000000-0000-0000-0000-000000000004', 'OUTBREAK_ALERT', 'Disease Outbreak Alert', 'URGENT: {diseaseName} Outbreak Cluster Detected', 'A {riskScore} risk outbreak of {diseaseName} has been detected within {radiusKm}km of your farm.', 'PUSH')
ON CONFLICT (code) DO NOTHING;
