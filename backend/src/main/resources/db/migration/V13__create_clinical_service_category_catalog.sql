INSERT INTO catalogs (code, name, description, specialty)
SELECT
    'CLINICAL_SERVICE_CATEGORY',
    'Categorías de servicios clínicos',
    'Categorías disponibles para clasificar los servicios clínicos',
    'GENERAL'
WHERE NOT EXISTS (
    SELECT 1 FROM catalogs WHERE code = 'CLINICAL_SERVICE_CATEGORY'
);

INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'EVALUACION', 'Evaluación', TRUE, 0
FROM catalogs
WHERE code = 'CLINICAL_SERVICE_CATEGORY'
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      JOIN catalogs catalog ON catalog.id = item.catalog_id
      WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY'
        AND item.item_code = 'EVALUACION'
  );

INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'TERAPIA', 'Terapia', TRUE, 1
FROM catalogs
WHERE code = 'CLINICAL_SERVICE_CATEGORY'
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      JOIN catalogs catalog ON catalog.id = item.catalog_id
      WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY'
        AND item.item_code = 'TERAPIA'
  );

INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'PROCEDIMIENTO', 'Procedimiento', TRUE, 2
FROM catalogs
WHERE code = 'CLINICAL_SERVICE_CATEGORY'
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      JOIN catalogs catalog ON catalog.id = item.catalog_id
      WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY'
        AND item.item_code = 'PROCEDIMIENTO'
  );

INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'CONTROL', 'Control', TRUE, 3
FROM catalogs
WHERE code = 'CLINICAL_SERVICE_CATEGORY'
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      JOIN catalogs catalog ON catalog.id = item.catalog_id
      WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY'
        AND item.item_code = 'CONTROL'
  );

INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'DIAGNOSTICO', 'Diagnóstico', TRUE, 4
FROM catalogs
WHERE code = 'CLINICAL_SERVICE_CATEGORY'
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      JOIN catalogs catalog ON catalog.id = item.catalog_id
      WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY'
        AND item.item_code = 'DIAGNOSTICO'
  );

INSERT INTO catalog_items (catalog_id, item_code, item_name, is_active, order_index)
SELECT id, 'OTRO', 'Otro', TRUE, 5
FROM catalogs
WHERE code = 'CLINICAL_SERVICE_CATEGORY'
  AND NOT EXISTS (
      SELECT 1
      FROM catalog_items item
      JOIN catalogs catalog ON catalog.id = item.catalog_id
      WHERE catalog.code = 'CLINICAL_SERVICE_CATEGORY'
        AND item.item_code = 'OTRO'
  );

UPDATE clinical_services
SET category = CASE UPPER(TRIM(category))
    WHEN 'EVALUACIÓN' THEN 'EVALUACION'
    WHEN 'EVALUACION' THEN 'EVALUACION'
    WHEN 'TERAPIA' THEN 'TERAPIA'
    WHEN 'PROCEDIMIENTO' THEN 'PROCEDIMIENTO'
    WHEN 'CONTROL' THEN 'CONTROL'
    WHEN 'DIAGNÓSTICO' THEN 'DIAGNOSTICO'
    WHEN 'DIAGNOSTICO' THEN 'DIAGNOSTICO'
    WHEN 'OTRO' THEN 'OTRO'
    ELSE category
END
WHERE UPPER(TRIM(category)) IN (
    'EVALUACIÓN',
    'EVALUACION',
    'TERAPIA',
    'PROCEDIMIENTO',
    'CONTROL',
    'DIAGNÓSTICO',
    'DIAGNOSTICO',
    'OTRO'
);
