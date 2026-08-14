-- V30: Recetas / indicaciones.

CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    prescription_date DATE,
    valid_until DATE,
    notes TEXT,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE TABLE prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    dose VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    route VARCHAR(100),
    instructions TEXT,
    CONSTRAINT fk_prescription_items_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_active_patient ON prescriptions(patient_id, deleted);
CREATE INDEX IF NOT EXISTS idx_prescription_items_prescription ON prescription_items(prescription_id);
