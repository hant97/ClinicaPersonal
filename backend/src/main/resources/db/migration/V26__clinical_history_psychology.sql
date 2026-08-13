-- V26: Bloque PSICOLOGÍA de la historia clínica.
-- psychology_evaluations cubre Evaluación inicial + Antecedentes psicológicos + Evaluación mental.

CREATE TABLE psychology_evaluations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    evaluation_date DATE NOT NULL,
    initial_evaluation TEXT,
    psychological_history TEXT,
    mental_exam TEXT,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_psychology_evaluations_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE diagnoses (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    category VARCHAR(100),
    description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVO',
    diagnosis_date DATE,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_diagnoses_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE therapeutic_plans (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    objectives TEXT,
    interventions TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVO',
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_therapeutic_plans_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Migración de datos (mejor esfuerzo): diagnóstico y plan de los registros médicos de psicología
-- se copian a las tablas normalizadas. Los registros originales se conservan hasta su deprecación.
INSERT INTO diagnoses (patient_id, specialty, description, status, diagnosis_date)
SELECT patient_id, specialty, diagnosis, 'ACTIVO', created_at::date
FROM medical_records
WHERE specialty = 'PSICOLOGIA'
  AND diagnosis IS NOT NULL AND diagnosis <> ''
  AND deleted = FALSE;

INSERT INTO therapeutic_plans (patient_id, specialty, notes, status, start_date)
SELECT patient_id, specialty, treatment_plan, 'ACTIVO', created_at::date
FROM medical_records
WHERE specialty = 'PSICOLOGIA'
  AND treatment_plan IS NOT NULL AND treatment_plan <> ''
  AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_psychology_evaluations_active_patient ON psychology_evaluations(patient_id, deleted);
CREATE INDEX IF NOT EXISTS idx_diagnoses_active_patient_specialty ON diagnoses(patient_id, specialty, deleted);
CREATE INDEX IF NOT EXISTS idx_therapeutic_plans_active_patient_specialty ON therapeutic_plans(patient_id, specialty, deleted);
