-- Índices para pacientes
CREATE INDEX IF NOT EXISTS idx_clinical_sessions_patient_id ON clinical_sessions(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_payments_patient_id ON payments(patient_id);
CREATE INDEX IF NOT EXISTS idx_risk_alerts_patient_id ON risk_alerts(patient_id);
CREATE INDEX IF NOT EXISTS idx_medical_records_patient_id ON medical_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_assessments_patient_id ON assessments(patient_id);

-- Otros índices de relaciones
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_assessments_test_id ON assessments(psychometric_test_id);
CREATE INDEX IF NOT EXISTS idx_catalog_items_catalog_id ON catalog_items(catalog_id);
CREATE INDEX IF NOT EXISTS idx_catalog_items_catalog_code ON catalog_items(catalog_code);