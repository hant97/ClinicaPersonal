-- Las sesiones, historias y evaluaciones son documentación clínica: se conservan
-- para trazabilidad y solo se excluyen de las consultas operativas.
ALTER TABLE clinical_sessions
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE clinical_sessions
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE clinical_sessions
    ADD COLUMN IF NOT EXISTS deleted_by BIGINT;

ALTER TABLE medical_records
    ADD COLUMN IF NOT EXISTS professional_id BIGINT;
ALTER TABLE medical_records
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE medical_records
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE medical_records
    ADD COLUMN IF NOT EXISTS deleted_by BIGINT;

ALTER TABLE dermatological_evaluations
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE dermatological_evaluations
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE dermatological_evaluations
    ADD COLUMN IF NOT EXISTS deleted_by BIGINT;

CREATE INDEX IF NOT EXISTS idx_clinical_sessions_active_patient_specialty
    ON clinical_sessions(patient_id, specialty, deleted);
CREATE INDEX IF NOT EXISTS idx_medical_records_active_patient_specialty
    ON medical_records(patient_id, specialty, deleted);
CREATE INDEX IF NOT EXISTS idx_dermatological_evaluations_active_patient_professional
    ON dermatological_evaluations(patient_id, professional_id, deleted);
