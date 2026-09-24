-- Búsqueda de pacientes sin distinción de tildes ni mayúsculas. La aplicación mantiene la
-- columna en cada alta o edición (SearchText.of); aquí se rellenan los registros existentes.
-- TRANSLATE funciona en PostgreSQL y en H2 (pruebas), a diferencia de la extensión unaccent.
ALTER TABLE patients ADD COLUMN search_text VARCHAR(700);

UPDATE patients
SET search_text = LOWER(TRIM(TRANSLATE(
        CONCAT(first_name, ' ', last_name, ' ', COALESCE(identification_document, '')),
        'ÁÀÄÂÉÈËÊÍÌÏÎÓÒÖÔÚÙÜÛÑÇáàäâéèëêíìïîóòöôúùüûñç',
        'AAAAEEEEIIIIOOOOUUUUNCaaaaeeeeiiiioooouuuunc')));
