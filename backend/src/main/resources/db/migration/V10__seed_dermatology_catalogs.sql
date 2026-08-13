-- V10: Insertar catálogos para dermatología

-- 1. Insertar catálogos
INSERT INTO catalogs (code, name, description, specialty) VALUES
('SKIN_TYPE', 'Tipo de Piel', 'Clasificación de tipos de piel', 'DERMATOLOGIA'),
('LESION_TYPE', 'Tipo de Lesión', 'Tipos de lesiones dermatológicas', 'DERMATOLOGIA'),
('BODY_AREA', 'Zona Afectada', 'Áreas del cuerpo', 'DERMATOLOGIA'),
('DERM_PROCEDURE', 'Procedimiento Dermatológico', 'Procedimientos comunes en dermatología', 'DERMATOLOGIA'),
('DERM_SESSION_TYPE', 'Tipo de Sesión', 'Tipos de sesión dermatológica', 'DERMATOLOGIA'),
('DERM_MODALITY', 'Modalidad de Cita', 'Modalidad de atención dermatológica', 'DERMATOLOGIA'),
('RISK_ALERT_TYPE_DERM', 'Tipo de Alerta de Riesgo', 'Riesgos dermatológicos', 'DERMATOLOGIA');

-- 2. Insertar items usando subconsultas

-- SKIN_TYPE
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PIEL_SECA', 'Piel Seca', true, 0 FROM catalogs WHERE code = 'SKIN_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PIEL_GRASA', 'Piel Grasa', true, 1 FROM catalogs WHERE code = 'SKIN_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PIEL_MIXTA', 'Piel Mixta', true, 2 FROM catalogs WHERE code = 'SKIN_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PIEL_NORMAL', 'Piel Normal', true, 3 FROM catalogs WHERE code = 'SKIN_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PIEL_SENSIBLE', 'Piel Sensible', true, 4 FROM catalogs WHERE code = 'SKIN_TYPE';

-- LESION_TYPE
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'MACULA', 'Mácula', true, 0 FROM catalogs WHERE code = 'LESION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PAPULA', 'Pápula', true, 1 FROM catalogs WHERE code = 'LESION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'NODULO', 'Nódulo', true, 2 FROM catalogs WHERE code = 'LESION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'VESICULA', 'Vesícula', true, 3 FROM catalogs WHERE code = 'LESION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PLACA', 'Placa', true, 4 FROM catalogs WHERE code = 'LESION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'ULCERA', 'Úlcera', true, 5 FROM catalogs WHERE code = 'LESION_TYPE';

-- BODY_AREA
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'ROSTRO', 'Rostro', true, 0 FROM catalogs WHERE code = 'BODY_AREA';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CUELLO', 'Cuello', true, 1 FROM catalogs WHERE code = 'BODY_AREA';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'TORAX', 'Tórax', true, 2 FROM catalogs WHERE code = 'BODY_AREA';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'ESPALDA', 'Espalda', true, 3 FROM catalogs WHERE code = 'BODY_AREA';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'EXTREMIDADES_SUPERIORES', 'Extremidades Superiores', true, 4 FROM catalogs WHERE code = 'BODY_AREA';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'EXTREMIDADES_INFERIORES', 'Extremidades Inferiores', true, 5 FROM catalogs WHERE code = 'BODY_AREA';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CUERO_CABELLUDO', 'Cuero Cabelludo', true, 6 FROM catalogs WHERE code = 'BODY_AREA';

-- DERM_PROCEDURE
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'BIOPSIA_DE_PIEL', 'Biopsia de Piel', true, 0 FROM catalogs WHERE code = 'DERM_PROCEDURE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CRIOTERAPIA', 'Crioterapia', true, 1 FROM catalogs WHERE code = 'DERM_PROCEDURE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'ELECTROCAUTERIZACION', 'Electrocauterización', true, 2 FROM catalogs WHERE code = 'DERM_PROCEDURE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PEELING_QUIMICO', 'Peeling Químico', true, 3 FROM catalogs WHERE code = 'DERM_PROCEDURE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'EXTIRPACION_QUIRURGICA', 'Extirpación Quirúrgica', true, 4 FROM catalogs WHERE code = 'DERM_PROCEDURE';

-- DERM_SESSION_TYPE
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CONSULTA_PRIMERA_VEZ', 'Consulta Primera Vez', true, 0 FROM catalogs WHERE code = 'DERM_SESSION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CONTROL_DE_RUTINA', 'Control de Rutina', true, 1 FROM catalogs WHERE code = 'DERM_SESSION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CONTROL_POST_PROCEDIMIENTO', 'Control Post-Procedimiento', true, 2 FROM catalogs WHERE code = 'DERM_SESSION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PROCEDIMIENTO_AMBULATORIO', 'Procedimiento Ambulatorio', true, 3 FROM catalogs WHERE code = 'DERM_SESSION_TYPE';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'EMERGENCIA', 'Emergencia', true, 4 FROM catalogs WHERE code = 'DERM_SESSION_TYPE';

-- DERM_MODALITY
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PRESENCIAL', 'Presencial', true, 0 FROM catalogs WHERE code = 'DERM_MODALITY';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'TELEMEDICINA', 'Telemedicina', true, 1 FROM catalogs WHERE code = 'DERM_MODALITY';

-- RISK_ALERT_TYPE_DERM
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'SOSPECHA_DE_MELANOMA', 'Sospecha de Melanoma', true, 0 FROM catalogs WHERE code = 'RISK_ALERT_TYPE_DERM';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'REACCION_ADVERSA_A_TRATAMIENTO', 'Reacción Adversa a Tratamiento', true, 1 FROM catalogs WHERE code = 'RISK_ALERT_TYPE_DERM';
INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'INFECCION_SEVERA', 'Infección Severa', true, 2 FROM catalogs WHERE code = 'RISK_ALERT_TYPE_DERM';
