-- ----------------------------------------------------------------------------
-- Fase 3: Agenda Avanzada
-- Recurrencia de citas, horarios semanales por profesional y bloqueos/vacaciones.
-- ----------------------------------------------------------------------------

-- Columnas de recurrencia en appointments
ALTER TABLE appointments ADD COLUMN recurrence_group_id VARCHAR(64);
ALTER TABLE appointments ADD COLUMN recurrence_rule VARCHAR(100);

CREATE INDEX idx_appointments_recurrence_group ON appointments(recurrence_group_id);
CREATE INDEX idx_appointments_professional_id ON appointments(professional_id);

-- Tabla de horarios semanales por profesional
CREATE TABLE professional_schedules (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL REFERENCES users(id),
    day_of_week INT NOT NULL, -- 1 = Lunes, 7 = Domingo (ISO-8601)
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    specialty VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prof_schedules_prof_specialty ON professional_schedules(professional_id, specialty);
CREATE INDEX idx_prof_schedules_day ON professional_schedules(day_of_week);

-- Tabla de bloqueos de agenda y vacaciones
CREATE TABLE schedule_blocks (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT REFERENCES users(id), -- NULL si aplica a toda la especialidad
    specialty VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schedule_blocks_dates ON schedule_blocks(start_date, end_date);
CREATE INDEX idx_schedule_blocks_prof_specialty ON schedule_blocks(professional_id, specialty);
