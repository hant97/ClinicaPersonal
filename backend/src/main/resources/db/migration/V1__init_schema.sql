-- ============================================================================
-- ESQUEMA CONSOLIDADO BASE DE BASE DE DATOS (POSTGRESQL)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. SEGURIDAD, USUARIOS Y SESIONES
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(150),
    phone VARCHAR(20),
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    token_version BIGINT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(255),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expiration ON refresh_tokens(expires_at);

CREATE TABLE revoked_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_id VARCHAR(100) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_revoked_tokens_id ON revoked_tokens(token_id);
CREATE INDEX idx_revoked_tokens_expiration ON revoked_tokens(expires_at);

-- ----------------------------------------------------------------------------
-- 2. PACIENTES Y AJUSTES CLÍNICOS
-- ----------------------------------------------------------------------------
CREATE TABLE clinic_settings (
    id BIGSERIAL PRIMARY KEY,
    clinic_name VARCHAR(255),
    short_name VARCHAR(255),
    logo_url VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(255),
    address VARCHAR(255),
    specialty VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    identification_document VARCHAR(255),
    date_of_birth DATE,
    contact_number VARCHAR(255),
    email VARCHAR(255),
    occupation VARCHAR(255),
    marital_status VARCHAR(255),
    emergency_contact VARCHAR(255),
    reason_for_consultation VARCHAR(500),
    gender VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    guardian_name VARCHAR(255),
    guardian_contact VARCHAR(255),
    has_legal_guardian BOOLEAN NOT NULL DEFAULT FALSE,
    photo_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_patients_specialty_identification_document UNIQUE (specialty, identification_document)
);

CREATE TABLE risk_alerts (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    type VARCHAR(50) NOT NULL,
    level VARCHAR(50) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_alerts_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX idx_risk_alerts_patient_id ON risk_alerts(patient_id);

-- ----------------------------------------------------------------------------
-- 3. CATÁLOGOS DEL SISTEMA
-- ----------------------------------------------------------------------------
CREATE TABLE catalogs (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA'
);

CREATE TABLE catalog_items (
    id BIGSERIAL PRIMARY KEY,
    catalog_id BIGINT NOT NULL REFERENCES catalogs(id) ON DELETE CASCADE,
    item_code VARCHAR(100) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    order_index INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_catalog_items_catalog_id ON catalog_items(catalog_id);

-- ----------------------------------------------------------------------------
-- 4. SERVICIOS, INVENTARIO Y OPERACIONES
-- ----------------------------------------------------------------------------
CREATE TABLE clinical_services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL DEFAULT '',
    description VARCHAR(500),
    price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    category VARCHAR(100),
    duration_minutes INTEGER,
    image_url VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE supplies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL DEFAULT '',
    description VARCHAR(500),
    current_stock INTEGER NOT NULL DEFAULT 0,
    min_stock_level INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(255),
    price NUMERIC(10, 2),
    expiration_date DATE,
    image_url VARCHAR(500),
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventory_transactions (
    id BIGSERIAL PRIMARY KEY,
    supply_id BIGINT NOT NULL REFERENCES supplies(id),
    quantity INTEGER NOT NULL,
    transaction_type VARCHAR(255) NOT NULL,
    transaction_reason VARCHAR(255) NOT NULL,
    reference_id VARCHAR(255),
    notes VARCHAR(255),
    transaction_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_inventory_transactions_supply_id ON inventory_transactions(supply_id);

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    appointment_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    status VARCHAR(255) NOT NULL,
    modality VARCHAR(255),
    video_call_link VARCHAR(255),
    professional_id BIGINT,
    is_first_time BOOLEAN NOT NULL DEFAULT FALSE,
    clinical_session_id BIGINT,
    clinical_service_id BIGINT REFERENCES clinical_services(id),
    notes TEXT,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA'
);

CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_clinical_service ON appointments(clinical_service_id);

CREATE TABLE clinical_sessions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
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
    specialty VARCHAR(50) NOT NULL,
    professional_id BIGINT,
    appointment_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinical_sessions_patient_id ON clinical_sessions(patient_id);
CREATE INDEX idx_clinical_sessions_active_patient_specialty ON clinical_sessions(patient_id, specialty, deleted);

CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    amount NUMERIC(10, 2) NOT NULL,
    payment_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(255),
    appointment_id BIGINT REFERENCES appointments(id),
    description VARCHAR(255),
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_payments_patient_id ON payments(patient_id);
CREATE INDEX idx_payments_appointment ON payments(appointment_id);

CREATE TABLE payment_items (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    description VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    supply_id BIGINT REFERENCES supplies(id),
    clinical_service_id BIGINT REFERENCES clinical_services(id)
);

CREATE INDEX idx_payment_items_payment_id ON payment_items(payment_id);

-- ----------------------------------------------------------------------------
-- 5. HISTORIA CLÍNICA GENERAL Y COMÚN
-- ----------------------------------------------------------------------------
CREATE TABLE general_history (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    pathological_history TEXT,
    surgical_history TEXT,
    family_history TEXT,
    habits TEXT,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_general_history_patient_specialty UNIQUE (patient_id, specialty)
);

CREATE INDEX idx_general_history_patient_specialty ON general_history(patient_id, specialty);

CREATE TABLE allergies (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    allergen TEXT NOT NULL,
    type VARCHAR(50),
    severity VARCHAR(50),
    reaction TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_allergies_active_patient_specialty ON allergies(patient_id, specialty, deleted);

CREATE TABLE medications (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    name TEXT NOT NULL,
    dose VARCHAR(100),
    frequency VARCHAR(100),
    start_date DATE,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_medications_active_patient_specialty ON medications(patient_id, specialty, deleted);

CREATE TABLE diagnoses (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    category VARCHAR(100),
    description TEXT NOT NULL,
    status VARCHAR(50) DEFAULT 'ACTIVO',
    diagnosis_date DATE,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_diagnoses_active_patient_specialty ON diagnoses(patient_id, specialty, deleted);

CREATE TABLE therapeutic_plans (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
    objectives TEXT,
    interventions TEXT,
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVO',
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_therapeutic_plans_active_patient_specialty ON therapeutic_plans(patient_id, specialty, deleted);

-- ----------------------------------------------------------------------------
-- 6. HISTORIA CLÍNICA PSICOLOGÍA Y PRUEBAS PSICOMÉTRICAS
-- ----------------------------------------------------------------------------
CREATE TABLE psychology_evaluations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    evaluation_date DATE NOT NULL,
    initial_evaluation TEXT,
    psychological_history TEXT,
    mental_exam TEXT,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_psychology_evaluations_active_patient ON psychology_evaluations(patient_id, deleted);

CREATE TABLE psychometric_tests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    questions_json TEXT NOT NULL,
    interpretation_json TEXT
);

CREATE TABLE assessments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    psychometric_test_id BIGINT NOT NULL REFERENCES psychometric_tests(id),
    assessment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_score INTEGER,
    answers_json TEXT NOT NULL,
    notes TEXT
);

CREATE INDEX idx_assessments_patient_id ON assessments(patient_id);
CREATE INDEX idx_assessments_psychometric_test_id ON assessments(psychometric_test_id);

-- ----------------------------------------------------------------------------
-- 7. HISTORIA CLÍNICA DERMATOLOGÍA
-- ----------------------------------------------------------------------------
CREATE TABLE dermatological_history (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    skin_type VARCHAR(50),
    sun_exposure_habits TEXT,
    personal_skin_history TEXT,
    family_skin_history TEXT,
    chronic_conditions TEXT,
    exam_findings TEXT,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_dermatological_history_patient UNIQUE (patient_id)
);

CREATE INDEX idx_dermatological_history_patient ON dermatological_history(patient_id);

CREATE TABLE dermatological_evaluations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
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
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dermatological_evaluation_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

CREATE INDEX idx_dermatological_evaluations_patient_id ON dermatological_evaluations(patient_id);
CREATE INDEX idx_dermatological_evaluations_active_patient_professional ON dermatological_evaluations(patient_id, professional_id, deleted);

CREATE TABLE lesions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    body_area VARCHAR(100),
    lesion_type VARCHAR(100),
    size VARCHAR(50),
    morphology VARCHAR(255),
    color VARCHAR(100),
    since_date DATE,
    evolution TEXT,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lesions_active_patient ON lesions(patient_id, deleted);

CREATE TABLE lesion_photos (
    id BIGSERIAL PRIMARY KEY,
    lesion_id BIGINT NOT NULL REFERENCES lesions(id) ON DELETE CASCADE,
    file_url VARCHAR(500) NOT NULL,
    description VARCHAR(255),
    taken_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lesion_photos_lesion ON lesion_photos(lesion_id);

CREATE TABLE auxiliary_exams (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    exam_type VARCHAR(100),
    description TEXT,
    result TEXT,
    exam_date DATE,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auxiliary_exams_active_patient ON auxiliary_exams(patient_id, deleted);

CREATE TABLE treatments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    dose VARCHAR(100),
    route VARCHAR(100),
    frequency VARCHAR(100),
    start_date DATE,
    end_date DATE,
    status VARCHAR(50) DEFAULT 'ACTIVO',
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_treatments_active_patient ON treatments(patient_id, deleted);

CREATE TABLE procedures (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    procedure_date DATE,
    notes TEXT,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_procedures_active_patient ON procedures(patient_id, deleted);

CREATE TABLE evolutions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    control_date DATE NOT NULL,
    clinical_notes TEXT,
    next_control_date DATE,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evolutions_active_patient ON evolutions(patient_id, deleted);

-- ----------------------------------------------------------------------------
-- 8. RECETAS MÉDICAS Y DOCUMENTOS CLÍNICOS
-- ----------------------------------------------------------------------------
CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL,
    prescription_date DATE,
    valid_until DATE,
    notes TEXT,
    professional_id BIGINT,
    verification_code VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prescriptions_active_patient ON prescriptions(patient_id, deleted);
CREATE UNIQUE INDEX idx_prescriptions_verification_code ON prescriptions(verification_code);

CREATE TABLE prescription_items (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    dose VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    route VARCHAR(100),
    instructions TEXT
);

CREATE INDEX idx_prescription_items_prescription ON prescription_items(prescription_id);

CREATE TABLE clinical_documents (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    specialty VARCHAR(50) NOT NULL,
    category VARCHAR(50),
    name VARCHAR(255) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100),
    size_bytes BIGINT,
    document_date DATE,
    professional_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_clinical_documents_active_patient ON clinical_documents(patient_id, deleted);

-- ----------------------------------------------------------------------------
-- 9. SITIO WEB PÚBLICO Y BORRADOR DEL EDITOR
-- ----------------------------------------------------------------------------
CREATE TABLE website_settings (
    id BIGSERIAL PRIMARY KEY,
    singleton_key VARCHAR(1) NOT NULL DEFAULT 'S',
    commercial_name VARCHAR(120) NOT NULL,
    tagline VARCHAR(180),
    description VARCHAR(500),
    logo_external_image_url VARCHAR(500),
    logo_asset_key VARCHAR(255),
    hero_eyebrow VARCHAR(120),
    hero_title VARCHAR(180) NOT NULL,
    hero_highlight VARCHAR(120),
    hero_description VARCHAR(500),
    hero_primary_button_text VARCHAR(80),
    hero_secondary_button_text VARCHAR(80),
    hero_external_image_url VARCHAR(500),
    hero_asset_key VARCHAR(255),
    approach_title VARCHAR(180),
    approach_highlight VARCHAR(120),
    approach_description VARCHAR(1000),
    approach_secondary_description VARCHAR(1000),
    approach_cta_text VARCHAR(80),
    approach_external_image_url VARCHAR(500),
    approach_asset_key VARCHAR(255),
    contact_heading VARCHAR(180),
    contact_description VARCHAR(500),
    contact_phone VARCHAR(40),
    contact_whatsapp VARCHAR(40),
    contact_email VARCHAR(180),
    contact_address VARCHAR(300),
    contact_hours VARCHAR(300),
    map_url VARCHAR(500),
    facebook_url VARCHAR(500),
    instagram_url VARCHAR(500),
    tiktok_url VARCHAR(500),
    linkedin_url VARCHAR(500),
    seo_title VARCHAR(180),
    seo_description VARCHAR(300),
    seo_site_name VARCHAR(120),
    seo_external_image_url VARCHAR(500),
    seo_asset_key VARCHAR(255),
    published_at TIMESTAMP,
    published_by BIGINT,
    published_revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_website_settings_singleton UNIQUE (singleton_key),
    CONSTRAINT ck_website_settings_singleton CHECK (singleton_key = 'S')
);

CREATE TABLE website_specialties (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(500),
    icon_code VARCHAR(40) NOT NULL DEFAULT 'STETHOSCOPE',
    display_order INTEGER NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_specialty_services (
    id BIGSERIAL PRIMARY KEY,
    specialty_id BIGINT NOT NULL REFERENCES website_specialties(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_benefits (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    icon_code VARCHAR(40) NOT NULL DEFAULT 'SPARKLES',
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_process_steps (
    id BIGSERIAL PRIMARY KEY,
    step_number INTEGER NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_professionals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    specialty VARCHAR(120),
    license_number VARCHAR(80),
    description VARCHAR(1000),
    experience VARCHAR(300),
    care_areas VARCHAR(500),
    photo_external_url VARCHAR(500),
    photo_asset_key VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE website_landing_drafts (
    id BIGSERIAL PRIMARY KEY,
    singleton_key VARCHAR(1) NOT NULL DEFAULT 'S',
    content JSONB NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    CONSTRAINT uq_website_landing_drafts_singleton UNIQUE (singleton_key),
    CONSTRAINT ck_website_landing_drafts_singleton CHECK (singleton_key = 'S')
);

-- ----------------------------------------------------------------------------
-- 10. DATOS INICIALES Y SEMILLAS
-- ----------------------------------------------------------------------------

-- Usuarios iniciales
INSERT INTO users (username, password, specialty, token_version, enabled) VALUES
('admin', '$2a$10$I9dHLFAtRUVzcA8ngzcITeN1q0R2SBfEry0THN8JPmMYwF0b6eAKW', 'PSICOLOGIA', 0, TRUE),
('admin_psico', '$2a$10$se0lzYQLOIPq7tlmDCKmjeru.B7wY9fiwy38y3ysbByq/EmMPI0zW', 'PSICOLOGIA', 0, TRUE),
('admin_derm', '$2a$10$7/MDrYR55wnkGdflK3n9NuFtGhYoONMU.vkRNn4Ox1i96xhtYNC82', 'DERMATOLOGIA', 0, TRUE);

-- Roles asignados
INSERT INTO user_roles (user_id, role)
SELECT u.id, 'ROLE_SITE_ADMIN' FROM users u WHERE u.username = 'admin'
UNION ALL SELECT u.id, 'ROLE_ADMIN' FROM users u WHERE u.username = 'admin_psico'
UNION ALL SELECT u.id, 'ROLE_ADMIN' FROM users u WHERE u.username = 'admin_derm';

-- Catálogos Dermatología
INSERT INTO catalogs (code, name, description, specialty) VALUES
('SKIN_TYPE', 'Tipo de Piel', 'Clasificación de tipos de piel', 'DERMATOLOGIA'),
('LESION_TYPE', 'Tipo de Lesión', 'Tipos de lesiones dermatológicas', 'DERMATOLOGIA'),
('BODY_AREA', 'Zona Afectada', 'Áreas del cuerpo', 'DERMATOLOGIA'),
('DERM_PROCEDURE', 'Procedimiento Dermatológico', 'Procedimientos comunes en dermatología', 'DERMATOLOGIA'),
('DERM_SESSION_TYPE', 'Tipo de Sesión', 'Tipos de sesión dermatológica', 'DERMATOLOGIA'),
('DERM_MODALITY', 'Modalidad de Cita', 'Modalidad de atención dermatológica', 'DERMATOLOGIA'),
('RISK_ALERT_TYPE_DERM', 'Tipo de Alerta de Riesgo', 'Riesgos dermatológicos', 'DERMATOLOGIA');

-- Elementos de Catálogo
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PIEL_SECA', 'Piel Seca', true, 0 FROM catalogs WHERE code = 'SKIN_TYPE'
UNION ALL SELECT id, 'PIEL_GRASA', 'Piel Grasa', true, 1 FROM catalogs WHERE code = 'SKIN_TYPE'
UNION ALL SELECT id, 'PIEL_MIXTA', 'Piel Mixta', true, 2 FROM catalogs WHERE code = 'SKIN_TYPE'
UNION ALL SELECT id, 'PIEL_NORMAL', 'Piel Normal', true, 3 FROM catalogs WHERE code = 'SKIN_TYPE'
UNION ALL SELECT id, 'PIEL_SENSIBLE', 'Piel Sensible', true, 4 FROM catalogs WHERE code = 'SKIN_TYPE'
UNION ALL SELECT id, 'MACULA', 'Mácula', true, 0 FROM catalogs WHERE code = 'LESION_TYPE'
UNION ALL SELECT id, 'PAPULA', 'Pápula', true, 1 FROM catalogs WHERE code = 'LESION_TYPE'
UNION ALL SELECT id, 'NODULO', 'Nódulo', true, 2 FROM catalogs WHERE code = 'LESION_TYPE'
UNION ALL SELECT id, 'VESICULA', 'Vesícula', true, 3 FROM catalogs WHERE code = 'LESION_TYPE'
UNION ALL SELECT id, 'PLACA', 'Placa', true, 4 FROM catalogs WHERE code = 'LESION_TYPE'
UNION ALL SELECT id, 'ULCERA', 'Úlcera', true, 5 FROM catalogs WHERE code = 'LESION_TYPE'
UNION ALL SELECT id, 'ROSTRO', 'Rostro', true, 0 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'CUELLO', 'Cuello', true, 1 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'TORAX', 'Tórax', true, 2 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'ESPALDA', 'Espalda', true, 3 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'EXTREMIDADES_SUPERIORES', 'Extremidades Superiores', true, 4 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'EXTREMIDADES_INFERIORES', 'Extremidades Inferiores', true, 5 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'CUERO_CABELLUDO', 'Cuero Cabelludo', true, 6 FROM catalogs WHERE code = 'BODY_AREA'
UNION ALL SELECT id, 'BIOPSIA_DE_PIEL', 'Biopsia de Piel', true, 0 FROM catalogs WHERE code = 'DERM_PROCEDURE'
UNION ALL SELECT id, 'CRIOTERAPIA', 'Crioterapia', true, 1 FROM catalogs WHERE code = 'DERM_PROCEDURE'
UNION ALL SELECT id, 'ELECTROCAUTERIZACION', 'Electrocauterización', true, 2 FROM catalogs WHERE code = 'DERM_PROCEDURE'
UNION ALL SELECT id, 'PEELING_QUIMICO', 'Peeling Químico', true, 3 FROM catalogs WHERE code = 'DERM_PROCEDURE'
UNION ALL SELECT id, 'EXTIRPACION_QUIRURGICA', 'Extirpación Quirúrgica', true, 4 FROM catalogs WHERE code = 'DERM_PROCEDURE'
UNION ALL SELECT id, 'CONSULTA_PRIMERA_VEZ', 'Consulta Primera Vez', true, 0 FROM catalogs WHERE code = 'DERM_SESSION_TYPE'
UNION ALL SELECT id, 'CONTROL_DE_RUTINA', 'Control de Rutina', true, 1 FROM catalogs WHERE code = 'DERM_SESSION_TYPE'
UNION ALL SELECT id, 'CONTROL_POST_PROCEDIMIENTO', 'Control Post-Procedimiento', true, 2 FROM catalogs WHERE code = 'DERM_SESSION_TYPE'
UNION ALL SELECT id, 'PROCEDIMIENTO_AMBULATORIO', 'Procedimiento Ambulatorio', true, 3 FROM catalogs WHERE code = 'DERM_SESSION_TYPE'
UNION ALL SELECT id, 'EMERGENCIA', 'Emergencia', true, 4 FROM catalogs WHERE code = 'DERM_SESSION_TYPE'
UNION ALL SELECT id, 'PRESENCIAL', 'Presencial', true, 0 FROM catalogs WHERE code = 'DERM_MODALITY'
UNION ALL SELECT id, 'TELEMEDICINA', 'Telemedicina', true, 1 FROM catalogs WHERE code = 'DERM_MODALITY'
UNION ALL SELECT id, 'SOSPECHA_DE_MELANOMA', 'Sospecha de Melanoma', true, 0 FROM catalogs WHERE code = 'RISK_ALERT_TYPE_DERM'
UNION ALL SELECT id, 'REACCION_ADVERSA_A_TRATAMIENTO', 'Reacción Adversa a Tratamiento', true, 1 FROM catalogs WHERE code = 'RISK_ALERT_TYPE_DERM'
UNION ALL SELECT id, 'INFECCION_SEVERA', 'Infección Severa', true, 2 FROM catalogs WHERE code = 'RISK_ALERT_TYPE_DERM';

-- Configuración del Sitio Web (Landing)
INSERT INTO website_settings (
    commercial_name, tagline, description, hero_eyebrow, hero_title, hero_highlight,
    hero_description, hero_primary_button_text, hero_secondary_button_text, hero_external_image_url,
    approach_title, approach_highlight, approach_description, approach_secondary_description,
    approach_cta_text, approach_external_image_url, contact_heading, contact_description,
    seo_title, seo_description, seo_site_name
) VALUES (
    'Clínica Personal',
    'Dermatología y Psicología para tu bienestar integral.',
    'Dos especialidades, un mismo propósito: acompañarte con conocimiento, respeto y calidez.',
    'Cuidado que empieza por escucharte',
    'Tu bienestar,',
    'en buenas manos.',
    'Atención profesional en Dermatología y Psicología, con una mirada integral, cercana y pensada para tu momento.',
    'Agendar una cita',
    'Conocer nuestros servicios',
    'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1400&h=1750&q=82',
    'Atenderte también es',
    'conocerte.',
    'Creemos que la atención profesional puede sentirse humana. Por eso construimos un espacio donde puedas hablar con confianza, resolver tus dudas y tomar decisiones informadas sobre tu salud.',
    'Cada consulta comienza escuchándote y continúa con un acompañamiento respetuoso, sin juicios y a tu ritmo.',
    'Conversemos',
    'https://images.unsplash.com/photo-1544168190-79c17527004f?auto=format&fit=crop&w=1200&h=900&q=80',
    'Tu bienestar merece atención profesional.',
    'Déjanos tus datos o escríbenos por nuestros canales de contacto.',
    'Clínica Personal | Dermatología y Psicología',
    'Atención profesional en Dermatología y Psicología, con una mirada integral y cercana.',
    'Clínica Personal'
);

INSERT INTO website_specialties (code, label, title, subtitle, icon_code, display_order)
VALUES
    ('DERMATOLOGIA', '01 / Piel y cabello', 'Dermatología', 'Conoce y cuida tu piel con una evaluación profesional, clara y adaptada a tus necesidades.', 'MICROSCOPE', 1),
    ('PSICOLOGIA', '02 / Mente y emociones', 'Psicología', 'Un espacio seguro para comprender lo que sientes y encontrar herramientas para tu bienestar.', 'BRAIN', 2);

INSERT INTO website_specialty_services (specialty_id, name, display_order)
SELECT id, service_name, service_order
FROM website_specialties s
JOIN (VALUES
    ('DERMATOLOGIA', 'Consulta dermatológica', 1),
    ('DERMATOLOGIA', 'Acné, dermatitis y manchas', 2),
    ('DERMATOLOGIA', 'Caída del cabello y cuidado de la piel', 3),
    ('DERMATOLOGIA', 'Evaluación de lesiones de piel', 4),
    ('PSICOLOGIA', 'Consulta y evaluación psicológica', 1),
    ('PSICOLOGIA', 'Terapia individual', 2),
    ('PSICOLOGIA', 'Manejo del estrés y la ansiedad', 3),
    ('PSICOLOGIA', 'Autoestima y bienestar emocional', 4)
) AS services(specialty_code, service_name, service_order) ON services.specialty_code = s.code;

INSERT INTO website_benefits (title, description, icon_code, display_order)
VALUES
    ('Atención personalizada', 'Escuchamos tu historia para ofrecerte una atención acorde a lo que necesitas.', 'HEART_HANDSHAKE', 1),
    ('Confianza y confidencialidad', 'Cuidamos tu privacidad y respetamos cada proceso personal.', 'SHIELD_CHECK', 2),
    ('Profesionalismo cercano', 'Conocimiento y experiencia comunicados de forma clara y humana.', 'STETHOSCOPE', 3),
    ('Reserva sencilla', 'Da el primer paso de manera simple, a tu ritmo y sin complicaciones.', 'SPARKLES', 4);

INSERT INTO website_process_steps (step_number, title, description, display_order)
VALUES
    (1, 'Elige tu especialidad', 'Dermatología o Psicología, según lo que hoy necesitas.', 1),
    (2, 'Solicita una cita', 'Escríbenos por el canal que prefieras.', 2),
    (3, 'Confirma tu horario', 'Coordinamos contigo el día y la hora más conveniente.', 3),
    (4, 'Recibe atención profesional', 'Encontrarás un espacio seguro y pensado para ti.', 4);

-- Borrador inicial sincronizado del Landing Page
INSERT INTO website_landing_drafts (singleton_key, content, revision)
VALUES (
    'S',
    '{"commercialName":"Cl\u00ednica Personal","tagline":"Dermatolog\u00eda y Psicolog\u00eda para tu bienestar integral.","description":"Dos especialidades, un mismo prop\u00f3sito: acompa\u00f1arte con conocimiento, respeto y calidez.","logoExternalImageUrl":null,"logoAssetKey":null,"heroEyebrow":"Cuidado que empieza por escucharte","heroTitle":"Tu bienestar,","heroHighlight":"en buenas manos.","heroDescription":"Atenci\u00f3n profesional en Dermatolog\u00eda y Psicolog\u00eda, con una mirada integral, cercana y pensada para tu momento.","heroPrimaryButtonText":"Agendar una cita","heroSecondaryButtonText":"Conocer nuestros servicios","heroExternalImageUrl":"https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1400&h=1750&q=82","heroAssetKey":null,"approachTitle":"Atenderte tambi\u00e9n es","approachHighlight":"conocerte.","approachDescription":"Creemos que la atenci\u00f3n profesional puede sentirse humana. Por eso construimos un espacio donde puedas hablar con confianza, resolver tus dudas y tomar decisiones informadas sobre tu salud.","approachSecondaryDescription":"Cada consulta comienza escuch\u00e1ndote y contin\u00faa con un acompa\u00f1amiento respetuoso, sin juicios y a tu ritmo.","approachCtaText":"Conversemos","approachExternalImageUrl":"https://images.unsplash.com/photo-1544168190-79c17527004f?auto=format&fit=crop&w=1200&h=900&q=80","approachAssetKey":null,"contactHeading":"Tu bienestar merece atenci\u00f3n profesional.","contactDescription":"D\u00e9janos tus datos o escr\u00edbenos por nuestros canales de contacto.","contactPhone":null,"contactWhatsapp":null,"contactEmail":null,"contactAddress":null,"contactHours":null,"mapUrl":null,"facebookUrl":null,"instagramUrl":null,"tiktokUrl":null,"linkedinUrl":null,"seoTitle":"Cl\u00ednica Personal | Dermatolog\u00eda y Psicolog\u00eda","seoDescription":"Atenci\u00f3n profesional en Dermatolog\u00eda y Psicolog\u00eda, con una mirada integral y cercana.","seoSiteName":"Cl\u00ednica Personal","seoExternalImageUrl":null,"seoAssetKey":null,"specialties":[{"id":1,"code":"DERMATOLOGIA","label":"01 / Piel y cabello","title":"Dermatolog\u00eda","visible":true,"iconCode":"MICROSCOPE","draftKey":"draft-spec-1","subtitle":"Conoce y cuida tu piel con una evaluaci\u00f3n profesional, clara y adaptada a tus necesidades.","displayOrder":1},{"id":2,"code":"PSICOLOGIA","label":"02 / Mente y emociones","title":"Psicolog\u00eda","visible":true,"iconCode":"BRAIN","draftKey":"draft-spec-2","subtitle":"Un espacio seguro para comprender lo que sientes y encontrar herramientas para tu bienestar.","displayOrder":2}],"benefits":[{"id":1,"title":"Atenci\u00f3n personalizada","active":true,"iconCode":"HEART_HANDSHAKE","draftKey":"draft-ben-1","description":"Escuchamos tu historia para ofrecerte una atenci\u00f3n acorde a lo que necesitas.","displayOrder":1},{"id":2,"title":"Confianza y confidencialidad","active":true,"iconCode":"SHIELD_CHECK","draftKey":"draft-ben-2","description":"Cuidamos tu privacidad y respetamos cada proceso personal.","displayOrder":2},{"id":3,"title":"Profesionalismo cercano","active":true,"iconCode":"STETHOSCOPE","draftKey":"draft-ben-3","description":"Conocimiento y experiencia comunicados de forma clara y humana.","displayOrder":3},{"id":4,"title":"Reserva sencilla","active":true,"iconCode":"SPARKLES","draftKey":"draft-ben-4","description":"Da el primer paso de manera simple, a tu ritmo y sin complicaciones.","displayOrder":4}],"processSteps":[{"id":1,"title":"Elige tu especialidad","active":true,"draftKey":"draft-step-1","stepNumber":1,"description":"Dermatolog\u00eda o Psicolog\u00eda, seg\u00fan lo que hoy necesitas.","displayOrder":1},{"id":2,"title":"Solicita una cita","active":true,"draftKey":"draft-step-2","stepNumber":2,"description":"Escr\u00edbenos por el canal que prefieras.","displayOrder":2},{"id":3,"title":"Confirma tu horario","active":true,"draftKey":"draft-step-3","stepNumber":3,"description":"Coordinamos contigo el d\u00eda y la hora m\u00e1s conveniente.","displayOrder":3},{"id":4,"title":"Recibe atenci\u00f3n profesional","active":true,"draftKey":"draft-step-4","stepNumber":4,"description":"Encontrar\u00e1s un espacio seguro y pensado para ti.","displayOrder":4}],"professionals":[]}',
    0
);
