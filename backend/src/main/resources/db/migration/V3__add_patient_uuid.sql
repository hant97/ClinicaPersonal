-- ============================================================================
-- MIGRACIÓN V3: AGREGAR UUID PÚBLICO A PACIENTES
-- ============================================================================

ALTER TABLE patients ADD COLUMN uuid UUID DEFAULT gen_random_uuid() NOT NULL;
ALTER TABLE patients ADD CONSTRAINT uq_patients_uuid UNIQUE (uuid);
CREATE INDEX idx_patients_uuid ON patients (uuid);
