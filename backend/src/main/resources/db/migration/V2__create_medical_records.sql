CREATE TABLE medical_records (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    diagnosis TEXT,
    current_medication TEXT,
    treatment_plan TEXT,
    treatment_status VARCHAR(50) DEFAULT 'Activo',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medical_records_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);

-- Migrar datos existentes (si tienen al menos algún diagnóstico o plan)
INSERT INTO medical_records (patient_id, diagnosis, current_medication, treatment_plan, treatment_status, created_at)
SELECT id, diagnosis, current_medication, treatment_plan, COALESCE(treatment_status, 'Activo'), created_at
FROM patients
WHERE diagnosis IS NOT NULL OR treatment_plan IS NOT NULL OR current_medication IS NOT NULL;

-- Eliminar columnas de la tabla patients
ALTER TABLE patients 
DROP COLUMN diagnosis,
DROP COLUMN current_medication,
DROP COLUMN treatment_plan,
DROP COLUMN treatment_status;
