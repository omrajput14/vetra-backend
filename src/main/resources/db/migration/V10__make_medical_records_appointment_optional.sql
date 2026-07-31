-- Flyway Migration V10: Make appointment_id optional on medical_records for AI scan EVMR entry integration
ALTER TABLE medical_records ALTER COLUMN appointment_id DROP NOT NULL;
