-- V31: Documentos clínicos.

CREATE TABLE clinical_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    document_date DATE,
    professional_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    CONSTRAINT fk_clinical_documents_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_clinical_documents_active_patient ON clinical_documents(patient_id, deleted);
