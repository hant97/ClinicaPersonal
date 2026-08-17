-- V32: Vincular cobros con citas (trazabilidad atención -> pago).

ALTER TABLE payments ADD COLUMN IF NOT EXISTS appointment_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_appointment'
    ) THEN
        ALTER TABLE payments
            ADD CONSTRAINT fk_payments_appointment
            FOREIGN KEY (appointment_id) REFERENCES appointments(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_payments_appointment ON payments(appointment_id);
