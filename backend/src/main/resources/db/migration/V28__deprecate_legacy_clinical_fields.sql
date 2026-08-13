-- V28: Deprecación física de campos legacy y corrección de migraciones previas.
-- Los datos ya migrados a tablas normalizadas se conservan. Este paso retira la
-- fuente duplicada, elimina el truncado a 255 caracteres de las migraciones V25/V27
-- y migra los campos dermatológicos que aún quedaban en clinical_sessions.

-- 1. Convertir a TEXT las columnas que V25/V27 truncaron a VARCHAR(255) y
--    re-migrar el texto completo desde las fuentes (aún presentes en este punto).

ALTER TABLE allergies ALTER COLUMN allergen TYPE TEXT;
ALTER TABLE medications ALTER COLUMN name TYPE TEXT;
ALTER TABLE treatments ALTER COLUMN name TYPE TEXT;
ALTER TABLE procedures ALTER COLUMN name TYPE TEXT;

DELETE FROM allergies WHERE notes = 'Migrado desde historial de tratamientos';
INSERT INTO allergies (patient_id, specialty, allergen, notes, active)
SELECT patient_id, specialty, known_allergies, 'Migrado desde historial de tratamientos', TRUE
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND known_allergies IS NOT NULL AND known_allergies <> ''
  AND deleted = FALSE;

DELETE FROM medications WHERE notes = 'Migrado desde historial de tratamientos';
INSERT INTO medications (patient_id, specialty, name, notes, active)
SELECT patient_id, specialty, current_medication, 'Migrado desde historial de tratamientos', TRUE
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND current_medication IS NOT NULL AND current_medication <> ''
  AND deleted = FALSE;

DELETE FROM treatments WHERE notes = 'Migrado desde evaluación dermatológica';
INSERT INTO treatments (patient_id, name, status, notes)
SELECT DISTINCT patient_id, treatment_indicated, 'ACTIVO', 'Migrado desde evaluación dermatológica'
FROM dermatological_evaluations
WHERE treatment_indicated IS NOT NULL AND treatment_indicated <> ''
  AND deleted = FALSE;

DELETE FROM procedures WHERE description = 'Migrado desde evaluación dermatológica';
INSERT INTO procedures (patient_id, name, description)
SELECT DISTINCT patient_id, procedure_performed, 'Migrado desde evaluación dermatológica'
FROM dermatological_evaluations
WHERE procedure_performed IS NOT NULL AND procedure_performed <> ''
  AND deleted = FALSE;

-- 2. Migrar huecos restantes de medical_records.

-- Medicación actual de psicología -> medications
INSERT INTO medications (patient_id, specialty, name, notes, active)
SELECT patient_id, specialty, current_medication, 'Migrado desde historial de tratamientos', TRUE
FROM medical_records
WHERE specialty = 'PSICOLOGIA'
  AND current_medication IS NOT NULL AND current_medication <> ''
  AND deleted = FALSE;

-- Diagnóstico de dermatología -> diagnoses
INSERT INTO diagnoses (patient_id, specialty, description, status, diagnosis_date)
SELECT patient_id, specialty, diagnosis, 'ACTIVO', created_at::date
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND diagnosis IS NOT NULL AND diagnosis <> ''
  AND deleted = FALSE;

-- Plan de tratamiento de dermatología -> treatments
INSERT INTO treatments (patient_id, name, status, notes)
SELECT patient_id, treatment_plan, 'ACTIVO', 'Migrado desde historial de tratamientos'
FROM medical_records
WHERE specialty = 'DERMATOLOGIA'
  AND treatment_plan IS NOT NULL AND treatment_plan <> ''
  AND deleted = FALSE;

-- 3. Migrar campos dermatológicos de clinical_sessions.

-- Diagnóstico dermatológico -> diagnoses
INSERT INTO diagnoses (patient_id, specialty, description, status, diagnosis_date)
SELECT DISTINCT patient_id, 'DERMATOLOGIA', dermatological_diagnosis, 'ACTIVO', session_date
FROM clinical_sessions
WHERE specialty = 'DERMATOLOGIA'
  AND dermatological_diagnosis IS NOT NULL AND dermatological_diagnosis <> ''
  AND deleted = FALSE;

-- Procedimientos realizados -> procedures
INSERT INTO procedures (patient_id, name, description, procedure_date)
SELECT DISTINCT patient_id, procedures_performed, 'Migrado desde sesión clínica', session_date
FROM clinical_sessions
WHERE specialty = 'DERMATOLOGIA'
  AND procedures_performed IS NOT NULL AND procedures_performed <> ''
  AND deleted = FALSE;

-- Prescripciones -> treatments
INSERT INTO treatments (patient_id, name, status, notes)
SELECT DISTINCT patient_id, prescriptions, 'ACTIVO', 'Migrado desde sesión clínica'
FROM clinical_sessions
WHERE specialty = 'DERMATOLOGIA'
  AND prescriptions IS NOT NULL AND prescriptions <> ''
  AND deleted = FALSE;

-- 4. Examen dermatológico: nuevo campo en dermatological_history.
ALTER TABLE dermatological_history ADD COLUMN IF NOT EXISTS exam_findings TEXT;

INSERT INTO dermatological_history (patient_id)
SELECT DISTINCT patient_id
FROM clinical_sessions
WHERE specialty = 'DERMATOLOGIA'
  AND skin_exam_findings IS NOT NULL AND skin_exam_findings <> ''
  AND deleted = FALSE
ON CONFLICT (patient_id) DO NOTHING;

UPDATE dermatological_history dh
SET exam_findings = sub.exam_findings
FROM (
    SELECT DISTINCT ON (patient_id) patient_id, skin_exam_findings AS exam_findings
    FROM clinical_sessions
    WHERE specialty = 'DERMATOLOGIA'
      AND skin_exam_findings IS NOT NULL AND skin_exam_findings <> ''
      AND deleted = FALSE
    ORDER BY patient_id, session_date DESC
) sub
WHERE dh.patient_id = sub.patient_id;

-- 5. Retirar estructuras legacy.
DROP TABLE IF EXISTS medical_records;

ALTER TABLE clinical_sessions
    DROP COLUMN IF EXISTS skin_exam_findings,
    DROP COLUMN IF EXISTS dermatological_diagnosis,
    DROP COLUMN IF EXISTS procedures_performed,
    DROP COLUMN IF EXISTS prescriptions;
