-- V33: Enriquecer servicios clínicos y vincularlos con citas.

ALTER TABLE clinical_services ADD COLUMN IF NOT EXISTS category VARCHAR(100);
ALTER TABLE clinical_services ADD COLUMN IF NOT EXISTS duration_minutes INTEGER;
ALTER TABLE clinical_services ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE clinical_services ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE clinical_services ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE appointments ADD COLUMN IF NOT EXISTS clinical_service_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_appointments_clinical_service'
    ) THEN
        ALTER TABLE appointments
            ADD CONSTRAINT fk_appointments_clinical_service
            FOREIGN KEY (clinical_service_id) REFERENCES clinical_services(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_appointments_clinical_service ON appointments(clinical_service_id);
