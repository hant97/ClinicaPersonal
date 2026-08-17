-- ============================================================================
-- MIGRACIÓN V2: TABLA MAESTRA DE ESPECIALIDADES
-- ============================================================================

CREATE TABLE specialties (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    icon VARCHAR(50) NOT NULL DEFAULT 'Sparkles',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_specialties_code ON specialties(code);
CREATE INDEX idx_specialties_active_order ON specialties(active, display_order);

-- Semilla de especialidades iniciales
INSERT INTO specialties (code, name, description, icon, active, display_order)
VALUES
    ('PSICOLOGIA', 'Psicología', 'Atención en salud mental, psicología clínica y psicometría', 'Brain', TRUE, 1),
    ('DERMATOLOGIA', 'Dermatología', 'Atención en dermatología clínica, procedimientos y cuidado de la piel', 'Stethoscope', TRUE, 2);
