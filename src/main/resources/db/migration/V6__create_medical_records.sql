-- ─────────────────────────────────────────────────────────────────────
-- V6__create_medical_records.sql
-- Electronic Veterinary Medical Record (EVMR) Module Schema
-- ─────────────────────────────────────────────────────────────────────

-- 1. Rename column vet_id to veterinarian_id if vet_id exists from legacy V2 schema
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'medical_records' AND column_name = 'vet_id'
    ) THEN
        ALTER TABLE medical_records RENAME COLUMN vet_id TO veterinarian_id;
    END IF;
END $$;

-- 2. Safely add missing columns if table already existed from V2 schema
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS appointment_id UUID;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS farmer_id UUID;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS veterinarian_id UUID;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS symptoms TEXT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS weight NUMERIC(6,2);
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS temperature NUMERIC(4,1);
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS follow_up_date DATE;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- 3. Create table if not exists
CREATE TABLE IF NOT EXISTS medical_records (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    appointment_id UUID UNIQUE,
    animal_id UUID NOT NULL,
    farmer_id UUID NOT NULL,
    veterinarian_id UUID NOT NULL,
    diagnosis TEXT NOT NULL,
    symptoms TEXT,
    treatment TEXT NOT NULL,
    prescription TEXT,
    weight NUMERIC(6,2),
    temperature NUMERIC(4,1),
    follow_up_date DATE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_medical_records_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_records_animal FOREIGN KEY (animal_id) REFERENCES animals(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_records_farmer FOREIGN KEY (farmer_id) REFERENCES farmer_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_medical_records_veterinarian FOREIGN KEY (veterinarian_id) REFERENCES vet_profiles(id) ON DELETE CASCADE
);

-- 4. Create Indexes safely
CREATE INDEX IF NOT EXISTS idx_medical_records_animal_id ON medical_records(animal_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_veterinarian_id ON medical_records(veterinarian_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_farmer_id ON medical_records(farmer_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_appointment_id ON medical_records(appointment_id);
