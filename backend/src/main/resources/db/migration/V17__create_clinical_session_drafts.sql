CREATE TABLE clinical_session_drafts (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES users(id),
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    content_json TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_clinical_session_drafts_professional_patient
        UNIQUE (professional_id, patient_id)
);

CREATE INDEX idx_clinical_session_drafts_expires_at
    ON clinical_session_drafts(expires_at);
