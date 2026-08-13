-- V27: Bloque DERMATOLOGÍA de la historia clínica.

CREATE TABLE dermatological_history (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    skin_type VARCHAR(50),
    sun_exposure_habits TEXT,
    personal_skin_history TEXT,
    family_skin_history TEXT,
    chronic_conditions TEXT,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_dermatological_history_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT uq_dermatological_history_patient UNIQUE (patient_id)
);

CREATE TABLE lesions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    body_area VARCHAR(100),
    lesion_type VARCHAR(100),
    size VARCHAR(50),
    morphology VARCHAR(255),
    color VARCHAR(100),
    since_date DATE,
    evolution TEXT,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_lesions_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE lesion_photos (
    id BIGSERIAL PRIMARY KEY,
    lesion_id BIGINT NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    taken_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesion_photos_lesion FOREIGN KEY (lesion_id) REFERENCES lesions(id) ON DELETE CASCADE
);

CREATE TABLE auxiliary_exams (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    exam_type VARCHAR(100),
    description TEXT,
    result TEXT,
    exam_date DATE,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_auxiliary_exams_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE treatments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    dose VARCHAR(100),
    route VARCHAR(100),
    frequency VARCHAR(100),
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
    CONSTRAINT fk_treatments_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE procedures (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    procedure_date DATE,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_procedures_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE evolutions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    control_date DATE NOT NULL,
    clinical_notes TEXT,
    next_control_date DATE,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_evolutions_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Migración de datos (mejor esfuerzo): copia de los campos dermatológicos dispersos
-- en medical_records y dermatological_evaluations hacia las tablas normalizadas.
INSERT INTO dermatological_history (patient_id, skin_type, sun_exposure_habits, chronic_conditions)
SELECT DISTINCT ON (patient_id) patient_id, skin_type, sun_exposure_habits, chronic_conditions
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND deleted = FALSE
  AND (skin_type IS NOT NULL OR sun_exposure_habits IS NOT NULL OR chronic_conditions IS NOT NULL)
ORDER BY patient_id, created_at DESC;

INSERT INTO diagnoses (patient_id, specialty, description, status, diagnosis_date)
SELECT DISTINCT patient_id, 'DERMATOLOGIA', dermatological_diagnosis, 'ACTIVO', evaluation_date
FROM dermatological_evaluations
WHERE dermatological_diagnosis IS NOT NULL AND dermatological_diagnosis <> ''
  AND deleted = FALSE;

INSERT INTO treatments (patient_id, name, status, notes)
SELECT DISTINCT patient_id, LEFT(treatment_indicated, 255), 'ACTIVO', 'Migrado desde evaluación dermatológica'
FROM dermatological_evaluations
WHERE treatment_indicated IS NOT NULL AND treatment_indicated <> ''
  AND deleted = FALSE;

INSERT INTO procedures (patient_id, name, description)
SELECT DISTINCT patient_id, LEFT(procedure_performed, 255), 'Migrado desde evaluación dermatológica'
FROM dermatological_evaluations
WHERE procedure_performed IS NOT NULL AND procedure_performed <> ''
  AND deleted = FALSE;

INSERT INTO evolutions (patient_id, control_date, clinical_notes, next_control_date)
SELECT patient_id, evaluation_date, evolution_notes, next_review_date
FROM dermatological_evaluations
WHERE deleted = FALSE
  AND (evolution_notes IS NOT NULL OR next_review_date IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_dermatological_history_patient ON dermatological_history(patient_id);
CREATE INDEX IF NOT EXISTS idx_lesions_active_patient ON lesions(patient_id, deleted);
CREATE INDEX IF NOT EXISTS idx_lesion_photos_lesion ON lesion_photos(lesion_id);
CREATE INDEX IF NOT EXISTS idx_auxiliary_exams_active_patient ON auxiliary_exams(patient_id, deleted);
CREATE INDEX IF NOT EXISTS idx_treatments_active_patient ON treatments(patient_id, deleted);
CREATE INDEX IF NOT EXISTS idx_procedures_active_patient ON procedures(patient_id, deleted);
CREATE INDEX IF NOT EXISTS idx_evolutions_active_patient ON evolutions(patient_id, deleted);
