-- ----------------------------------------------------------------------------
-- Fase 1: Recordatorios y confirmación de citas
-- Campos para el envío único de recordatorios por correo y la confirmación
-- pública de la cita mediante un token.
-- ----------------------------------------------------------------------------
ALTER TABLE appointments ADD COLUMN reminder_sent_at TIMESTAMP;
ALTER TABLE appointments ADD COLUMN confirmation_token VARCHAR(255);
ALTER TABLE appointments ADD COLUMN confirmed_at TIMESTAMP;

CREATE INDEX idx_appointments_confirmation_token ON appointments(confirmation_token);
CREATE INDEX idx_appointments_reminder_sent ON appointments(reminder_sent_at);
