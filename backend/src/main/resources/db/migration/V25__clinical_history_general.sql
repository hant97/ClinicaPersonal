-- V25: Bloques generales de la historia clínica (Antecedentes generales, Alergias, Medicamentos).
-- Son bloques compartidos por ambas especialidades, almacenados por especialidad para no
-- mezclar datos entre consultorios (igual que medical_records y clinical_sessions).

CREATE TABLE general_history (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    pathological_history TEXT,
    surgical_history TEXT,
    family_history TEXT,
    habits TEXT,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_general_history_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT uq_general_history_patient_specialty UNIQUE (patient_id, specialty)
);

CREATE TABLE allergies (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    allergen VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    severity VARCHAR(50),
    reaction TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_allergies_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    name VARCHAR(255) NOT NULL,
    dose VARCHAR(100),
    frequency VARCHAR(100),
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_medications_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Migración de datos (mejor esfuerzo): el texto libre de los registros médicos dermatológicos
-- se copia a un único registro por paciente. Luego se podrá dividir en múltiples entradas.
INSERT INTO allergies (patient_id, specialty, allergen, notes, active)
SELECT patient_id, specialty, LEFT(known_allergies, 255), 'Migrado desde historial de tratamientos', TRUE
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND known_allergies IS NOT NULL AND known_allergies <> ''
  AND deleted = FALSE;

INSERT INTO medications (patient_id, specialty, name, notes, active)
SELECT patient_id, specialty, LEFT(current_medication, 255), 'Migrado desde historial de tratamientos', TRUE
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND current_medication IS NOT NULL AND current_medication <> ''
  AND deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_general_history_patient_specialty ON general_history(patient_id, specialty);
CREATE INDEX IF NOT EXISTS idx_allergies_active_patient_specialty ON allergies(patient_id, specialty, deleted);
CREATE INDEX IF NOT EXISTS idx_medications_active_patient_specialty ON medications(patient_id, specialty, deleted);
