-- V7: Añadir campos de dermatología a sesiones clínicas
ALTER TABLE clinical_sessions 
ADD COLUMN specialty VARCHAR(50) NOT NULL DEFAULT 'PSICOLOGIA',
ADD COLUMN skin_exam_findings TEXT,
ADD COLUMN dermatological_diagnosis TEXT,
ADD COLUMN procedures_performed TEXT,
ADD COLUMN prescriptions TEXT;
