CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    status VARCHAR(255) NOT NULL,
    modality VARCHAR(255),
    video_call_link VARCHAR(255),
    professional_id BIGINT,
    is_first_time BOOLEAN NOT NULL DEFAULT FALSE,
    clinical_session_id BIGINT,
    notes TEXT,
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

DO $$ 
BEGIN 
    BEGIN
        ALTER TABLE appointments ADD COLUMN start_time TIME;
    EXCEPTION WHEN duplicate_column THEN END;

    BEGIN
        ALTER TABLE appointments ADD COLUMN end_time TIME;
    EXCEPTION WHEN duplicate_column THEN END;

    BEGIN
        ALTER TABLE appointments ADD COLUMN modality VARCHAR(255);
    EXCEPTION WHEN duplicate_column THEN END;

    BEGIN
        ALTER TABLE appointments ADD COLUMN video_call_link VARCHAR(255);
    EXCEPTION WHEN duplicate_column THEN END;

    BEGIN
        ALTER TABLE appointments ADD COLUMN professional_id BIGINT;
    EXCEPTION WHEN duplicate_column THEN END;

    BEGIN
        ALTER TABLE appointments ADD COLUMN is_first_time BOOLEAN NOT NULL DEFAULT FALSE;
    EXCEPTION WHEN duplicate_column THEN END;

    BEGIN
        ALTER TABLE appointments ADD COLUMN clinical_session_id BIGINT;
    EXCEPTION WHEN duplicate_column THEN END;
END $$;

ALTER TABLE appointments ALTER COLUMN appointment_date TYPE DATE;

UPDATE appointments SET status = 'PROGRAMADA' WHERE status IS NULL;
ALTER TABLE appointments ALTER COLUMN status SET NOT NULL;
