-- Flyway Migration V7: Create partial availability index for vet directory searches
-- NOTE: vet_profiles does not have a 'district' column (district is on farmer_profiles).
-- Index uses (is_available) with spatial columns for available vet lookups.
CREATE INDEX IF NOT EXISTS idx_vet_profiles_available_spatial
ON vet_profiles (is_available, latitude, longitude)
WHERE is_available = TRUE;
