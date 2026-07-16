CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    identification_document VARCHAR(255) UNIQUE,
    date_of_birth DATE,
    contact_number VARCHAR(255),
    email VARCHAR(255),
    occupation VARCHAR(255),
    marital_status VARCHAR(255),
    emergency_contact VARCHAR(255),
    reason_for_consultation VARCHAR(500),
    gender VARCHAR(255),
    address VARCHAR(255),
    guardian_name VARCHAR(255),
    guardian_contact VARCHAR(255),
    has_legal_guardian BOOLEAN NOT NULL DEFAULT FALSE,
    diagnosis TEXT,
    current_medication TEXT,
    treatment_plan TEXT,
    treatment_status VARCHAR(50) DEFAULT 'Activo',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE clinical_sessions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    session_type VARCHAR(255) NOT NULL,
    modality VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    subjective TEXT,
    objective TEXT,
    analysis TEXT,
    plan TEXT,
    is_confidential BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    professional_id BIGINT,
    appointment_id BIGINT,
    CONSTRAINT fk_clinical_sessions_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_date TIMESTAMP NOT NULL,
    status VARCHAR(255),
    notes TEXT,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(255),
    description VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_payments_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

CREATE TABLE risk_alerts (
    id SERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_alerts_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);
