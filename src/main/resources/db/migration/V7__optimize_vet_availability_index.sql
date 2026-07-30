-- Flyway Migration V7: Create partial availability index for vet directory searches
CREATE INDEX IF NOT EXISTS idx_vet_profiles_available_spatial
ON vet_profiles (is_available, district)
WHERE is_available = TRUE;
