-- V8: Añadir campos de dermatología a registros médicos
ALTER TABLE medical_records 
ADD COLUMN specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
ADD COLUMN skin_type VARCHAR(50),
ADD COLUMN known_allergies TEXT,
ADD COLUMN chronic_conditions TEXT,
ADD COLUMN sun_exposure_habits TEXT;
