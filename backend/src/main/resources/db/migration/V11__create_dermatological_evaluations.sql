CREATE TABLE dermatological_evaluations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    evaluation_date DATE NOT NULL,
    skin_type VARCHAR(100),
    affected_area VARCHAR(100),
    lesion_type VARCHAR(100),
    lesion_size VARCHAR(50),
    dermatological_diagnosis TEXT,
    treatment_indicated TEXT,
    procedure_performed TEXT,
    evolution_notes TEXT,
    next_review_date DATE,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dermatological_evaluation_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX idx_dermatological_evaluations_patient_id ON dermatological_evaluations(patient_id);
