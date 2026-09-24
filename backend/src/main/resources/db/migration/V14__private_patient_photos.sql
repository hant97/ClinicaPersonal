-- Las fotos de pacientes pasan del almacenamiento público del sitio web al almacenamiento
-- clínico privado y se sirven en GET /api/v1/patients/{id}/photo (autenticado y filtrado por
-- especialidad). Se guarda la clave del archivo; la URL se calcula al responder.
ALTER TABLE patients ADD COLUMN photo_key VARCHAR(255);

-- 'patients/<uuid>.<ext>' conserva su valor como clave clínica. Si existen archivos locales
-- previos, deben moverse de uploads/website/patients/ a uploads/clinical/patients/.
UPDATE patients
SET photo_key = SUBSTRING(photo_url, LENGTH('/api/v1/public/website-assets/') + 1)
WHERE photo_url LIKE '/api/v1/public/website-assets/patients/%';

-- photo_url deja de usarse. Se conserva la columna (sin mapear) para no perder URLs externas
-- que pudieran haberse registrado por API; puede eliminarse en una migración posterior.
UPDATE patients SET photo_url = NULL WHERE photo_key IS NOT NULL;
