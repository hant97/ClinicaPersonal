-- ----------------------------------------------------------------------------
-- Fase 0: Pagos → facturación
-- Estado del cobro (PENDIENTE/PARCIAL/PAGADO), fecha de vencimiento, vínculo a
-- sesión clínica y abonos parciales (payment_transactions).
-- ----------------------------------------------------------------------------
ALTER TABLE payments ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PAGADO';
ALTER TABLE payments ADD COLUMN due_date DATE;
ALTER TABLE payments ADD COLUMN clinical_session_id BIGINT REFERENCES clinical_sessions(id);

CREATE INDEX idx_payments_status ON payments(status);

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL,
    transaction_date TIMESTAMP NOT NULL,
    payment_method VARCHAR(255),
    notes VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions(payment_id);
CREATE INDEX idx_payment_transactions_date ON payment_transactions(transaction_date);

-- Backfill: cada cobro existente queda PAGADO con un abono único equivalente al
-- monto original, conservando fecha y método para no alterar reportes históricos.
INSERT INTO payment_transactions (payment_id, amount, transaction_date, payment_method, notes)
SELECT id, amount, payment_date, payment_method, 'Abono migrado del cobro original'
FROM payments;
