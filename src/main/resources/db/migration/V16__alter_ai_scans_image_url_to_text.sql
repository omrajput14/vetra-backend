-- V16__alter_ai_scans_image_url_to_text.sql
-- 1. Alter image_url column on ai_scans from VARCHAR(512) to TEXT to support Base64 data URIs
ALTER TABLE ai_scans ALTER COLUMN image_url TYPE TEXT;

-- 2. Make prediction and confidence_score nullable on ai_scans (scans start in PENDING state before AI inference)
ALTER TABLE ai_scans ALTER COLUMN prediction DROP NOT NULL;
ALTER TABLE ai_scans ALTER COLUMN confidence_score DROP NOT NULL;
