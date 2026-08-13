-- Añadir campo specialty a clinical_services
CREATE TABLE IF NOT EXISTS clinical_services (
    id BIGSERIAL PRIMARY KEY
);

ALTER TABLE clinical_services ADD COLUMN IF NOT EXISTS specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA';

-- Añadir campo specialty a supplies (Inventario)
CREATE TABLE IF NOT EXISTS supplies (
    id BIGSERIAL PRIMARY KEY
);

ALTER TABLE supplies ADD COLUMN IF NOT EXISTS specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA';

-- Añadir campo specialty a appointments (Agenda)
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA';

-- Añadir campo specialty a payments (Cobros)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA';
