-- ============================================================================
-- MIGRACIÓN V4: AGREGAR CAMPOS DE SINCRONIZACIÓN CON GOOGLE CALENDAR
-- ============================================================================

ALTER TABLE appointments ADD COLUMN google_event_id VARCHAR(255);
ALTER TABLE appointments ADD COLUMN google_event_link VARCHAR(500);

CREATE INDEX idx_appointments_google_event_id ON appointments(google_event_id);
