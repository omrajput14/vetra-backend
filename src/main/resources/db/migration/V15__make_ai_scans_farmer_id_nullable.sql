-- V15__make_ai_scans_farmer_id_nullable.sql
-- Make legacy V2 farmer_id column nullable on ai_scans table to allow V8 AIScan entity persistence
ALTER TABLE ai_scans ALTER COLUMN farmer_id DROP NOT NULL;
