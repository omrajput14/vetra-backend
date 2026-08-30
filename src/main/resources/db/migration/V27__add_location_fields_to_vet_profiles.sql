-- V27: Add granular administrative location fields to vet_profiles
-- Enables hierarchical GPS and administrative location matching (Village, Taluka, District, State)

ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS village VARCHAR(100);
ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS taluka VARCHAR(100);
ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE vet_profiles ADD COLUMN IF NOT EXISTS state VARCHAR(100);

-- Spatial and administrative compound index for locality queries
CREATE INDEX IF NOT EXISTS idx_vet_profiles_location ON vet_profiles (taluka, district);
CREATE INDEX IF NOT EXISTS idx_vet_profiles_village ON vet_profiles (village);
