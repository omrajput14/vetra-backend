-- V22: Add Taluka (Sub-district / Block) to farmer_profiles table for granular regional health intelligence
ALTER TABLE farmer_profiles ADD COLUMN IF NOT EXISTS taluka VARCHAR(100);
