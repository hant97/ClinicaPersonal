-- ----------------------------------------------------------------------------
-- Evaluación de riesgo estructurada (Psicología)
-- Vinculada 1:1 (opcional) a la sesión clínica en la que se documenta;
-- se exige cuando el paciente tiene una alerta de riesgo activa.
-- ----------------------------------------------------------------------------

CREATE TABLE risk_assessments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    clinical_session_id BIGINT UNIQUE REFERENCES clinical_sessions(id),
    professional_id BIGINT REFERENCES users(id),
    suicidal_ideation BOOLEAN,
    ideation_frequency VARCHAR(100),
    has_plan BOOLEAN,
    plan_description TEXT,
    means_access BOOLEAN,
    means_description TEXT,
    protective_factors TEXT,
    risk_level VARCHAR(50),
    action_taken TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_assessments_patient_id ON risk_assessments(patient_id);
CREATE INDEX idx_risk_assessments_session_id ON risk_assessments(clinical_session_id);
CREATE INDEX idx_risk_assessments_deleted ON risk_assessments(deleted);
