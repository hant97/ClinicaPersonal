ALTER TABLE supplies
    ADD CONSTRAINT chk_supplies_current_stock_nonnegative
    CHECK (current_stock >= 0);

CREATE INDEX idx_appointments_conflict_lookup
    ON appointments(specialty, appointment_date, professional_id, status, start_time, end_time);
