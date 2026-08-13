-- V24: Segmentar pacientes por especialidad (opción A).
ALTER TABLE patients ADD COLUMN IF NOT EXISTS specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA';

-- Quitar la unicidad global del documento de identificación y permitir el mismo
-- documento por especialidad (el mismo paciente puede registrarse en psicología
-- y dermatología por separado).
ALTER TABLE patients DROP CONSTRAINT IF EXISTS patients_identification_document_key;

ALTER TABLE patients ADD CONSTRAINT uq_patients_specialty_identification_document
    UNIQUE (specialty, identification_document);
