import {
  buildInitialTransactions,
  buildPaymentPayload,
  computeLineTotal,
  mapPaymentItems,
  sumLines
} from './payment-form.utils';

describe('payment-form.utils', () => {
  it('calcula el total de una línea y suma varias líneas', () => {
    expect(computeLineTotal(2, 35)).toBe(70);
    expect(computeLineTotal(0, 35)).toBe(0);
    expect(sumLines([
      { quantity: 2, unitPrice: 35 },
      { quantity: 1, unitPrice: 12.5 }
    ])).toBe(82.5);
    expect(sumLines([])).toBe(0);
  });

  it('mapea servicios e insumos a los conceptos del cobro', () => {
    const items = mapPaymentItems(
      [{ description: 'Consulta', quantity: 2, unitPrice: 50, totalPrice: 100, clinicalServiceId: 7 }],
      [{ description: 'Crema', quantity: 1, unitPrice: 25, totalPrice: 25, supplyId: 9 }]
    );

    expect(items).toEqual([
      { description: 'Consulta', quantity: 2, unitPrice: 50, totalPrice: 100, clinicalServiceId: 7 },
      { description: 'Crema', quantity: 1, unitPrice: 25, totalPrice: 25, supplyId: 9 }
    ]);
  });

  it('construye la transacción de un cobro completo', () => {
    expect(buildInitialTransactions('FULL', 120, 120, '2026-09-28', 'EFECTIVO')).toEqual({
      transactions: [{ amount: 120, transactionDate: '2026-09-28', paymentMethod: 'EFECTIVO' }]
    });
  });

  it('valida y construye el abono inicial de un cobro parcial', () => {
    expect(buildInitialTransactions('PARTIAL', 120, 0, '2026-09-28', 'TARJETA')).toEqual({
      error: 'Ingrese un monto de abono inicial mayor a 0.'
    });
    expect(buildInitialTransactions('PARTIAL', 120, 120, '2026-09-28', 'TARJETA')).toEqual({
      error: 'El abono inicial debe ser menor al total del cobro.'
    });
    expect(buildInitialTransactions('PARTIAL', 120, 45, '2026-09-28', 'TARJETA')).toEqual({
      transactions: [{ amount: 45, transactionDate: '2026-09-28', paymentMethod: 'TARJETA' }]
    });
  });

  it('deja sin transacciones un cobro pendiente', () => {
    expect(buildInitialTransactions('PENDING', 120, 0, '2026-09-28', 'EFECTIVO')).toEqual({
      transactions: undefined
    });
  });

  it('normaliza fechas y relaciones opcionales al construir el cobro', () => {
    const items = [{ description: 'Consulta', quantity: 1, unitPrice: 120, totalPrice: 120 }];

    expect(buildPaymentPayload({
      patientId: 3,
      amount: 120,
      paymentDate: '2026-09-28',
      paymentMethod: 'EFECTIVO',
      dueDate: '',
      appointmentId: null,
      attentionId: 8,
      items
    })).toEqual({
      patientId: 3,
      amount: 120,
      paymentDate: '2026-09-28',
      paymentMethod: 'EFECTIVO',
      description: undefined,
      dueDate: undefined,
      appointmentId: undefined,
      attentionId: 8,
      items,
      transactions: undefined
    });
  });
});
