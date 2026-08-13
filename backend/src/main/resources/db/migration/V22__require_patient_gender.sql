-- V22: El género pasa a ser obligatorio en pacientes, alineado con el frontend.
UPDATE patients SET gender = 'PREFIERO_NO_DECIRLO' WHERE gender IS NULL OR gender = '';

ALTER TABLE patients ALTER COLUMN gender SET NOT NULL;
