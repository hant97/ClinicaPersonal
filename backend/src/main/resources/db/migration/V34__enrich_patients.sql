-- V34: Enriquecer pacientes (foto y estado activo/inactivo).

ALTER TABLE patients ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);
ALTER TABLE patients ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
