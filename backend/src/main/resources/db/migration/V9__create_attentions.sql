-- ----------------------------------------------------------------------------
-- Fase 4: Atenciones (Formalizar)
-- Entidad de atención con ciclo AGENDADA -> EN_PROCESO -> ATENDIDA -> COBRADA
-- y enlaces bidireccionales cita / sesión / receta / pago.
-- ----------------------------------------------------------------------------

CREATE TABLE attentions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    professional_id BIGINT NOT NULL REFERENCES users(id),
    appointment_id BIGINT REFERENCES appointments(id),
    clinical_session_id BIGINT REFERENCES clinical_sessions(id),
    prescription_id BIGINT REFERENCES prescriptions(id),
    payment_id BIGINT REFERENCES payments(id),
    clinical_service_id BIGINT REFERENCES clinical_services(id),
    specialty VARCHAR(50) NOT NULL,
    attention_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    duration_minutes INT,
    status VARCHAR(30) NOT NULL DEFAULT 'AGENDADA',
    motive VARCHAR(255),
    notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attentions_patient_id ON attentions(patient_id);
CREATE INDEX idx_attentions_prof_specialty ON attentions(professional_id, specialty);
CREATE INDEX idx_attentions_date ON attentions(attention_date);
CREATE INDEX idx_attentions_status ON attentions(status);
CREATE INDEX idx_attentions_appointment ON attentions(appointment_id);
CREATE INDEX idx_attentions_session ON attentions(clinical_session_id);
CREATE INDEX idx_attentions_prescription ON attentions(prescription_id);
CREATE INDEX idx_attentions_payment ON attentions(payment_id);
CREATE INDEX idx_attentions_deleted ON attentions(deleted);

-- Columnas de enlace bidireccional en tablas existentes
ALTER TABLE appointments ADD COLUMN attention_id BIGINT;
ALTER TABLE clinical_sessions ADD COLUMN attention_id BIGINT;
ALTER TABLE prescriptions ADD COLUMN attention_id BIGINT;
ALTER TABLE payments ADD COLUMN attention_id BIGINT;

CREATE INDEX idx_appointments_attention_id ON appointments(attention_id);
CREATE INDEX idx_clinical_sessions_attention_id ON clinical_sessions(attention_id);
CREATE INDEX idx_prescriptions_attention_id ON prescriptions(attention_id);
CREATE INDEX idx_payments_attention_id ON payments(attention_id);
