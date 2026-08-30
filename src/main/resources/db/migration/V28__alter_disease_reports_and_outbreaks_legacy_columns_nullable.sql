-- V28: Make legacy V2 columns nullable on disease_reports and outbreaks tables
-- Enables persistence of modern DiseaseReport and Outbreak surveillance entities

ALTER TABLE disease_reports ALTER COLUMN farmer_id DROP NOT NULL;
ALTER TABLE disease_reports ALTER COLUMN status DROP NOT NULL;

ALTER TABLE outbreaks ALTER COLUMN center_location DROP NOT NULL;
ALTER TABLE outbreaks ALTER COLUMN declared_by_vet_id DROP NOT NULL;
