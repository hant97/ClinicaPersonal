import { Payment, PaymentItem, PaymentTransaction } from '../../../core/models/payment.model';

/**
 * Funciones puras usadas por el formulario de cobros: cálculo de totales,
 * mapeo de líneas del formulario a `PaymentItem` y construcción de las
 * transacciones iniciales según el modo de pago elegido.
 * Extraídas del componente para poder probarlas de forma aislada.
 */

export interface PaymentLineValue {
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  clinicalServiceId?: number | null;
  supplyId?: number | null;
}

export function computeLineTotal(quantity: number, unitPrice: number): number {
  return Number(quantity || 0) * Number(unitPrice || 0);
}

export function sumLines(lines: { quantity: number; unitPrice: number }[]): number {
  return lines.reduce((total, line) => total + computeLineTotal(line.quantity, line.unitPrice), 0);
}

export function mapPaymentItems(services: PaymentLineValue[], supplies: PaymentLineValue[]): PaymentItem[] {
  const mappedServices: PaymentItem[] = services.map(item => ({
    description: item.description,
    quantity: Number(item.quantity),
    unitPrice: Number(item.unitPrice),
    totalPrice: computeLineTotal(item.quantity, item.unitPrice),
    clinicalServiceId: Number(item.clinicalServiceId)
  }));

  const mappedSupplies: PaymentItem[] = supplies.map(item => ({
    description: item.description,
    quantity: Number(item.quantity),
    unitPrice: Number(item.unitPrice),
    totalPrice: computeLineTotal(item.quantity, item.unitPrice),
    supplyId: Number(item.supplyId)
  }));

  return [...mappedServices, ...mappedSupplies];
}

export type PaymentMode = 'FULL' | 'PARTIAL' | 'PENDING';

export interface BuildTransactionsResult {
  transactions?: PaymentTransaction[];
  error?: string;
}

/**
 * Construye las transacciones iniciales de un cobro nuevo según el modo
 * de pago: cobro completo, abono parcial o pendiente (sin transacciones).
 * Devuelve `error` cuando el monto ingresado no es válido, para que el
 * componente lo muestre sin tener que repetir las reglas de validación.
 */
export function buildInitialTransactions(
  mode: PaymentMode,
  totalAmount: number,
  initialPayment: number,
  paymentDate: string,
  paymentMethod: string
): BuildTransactionsResult {
  if (mode === 'FULL') {
    return { transactions: [{ amount: totalAmount, transactionDate: paymentDate, paymentMethod }] };
  }

  if (mode === 'PARTIAL') {
    const initial = Number(initialPayment || 0);
    if (initial <= 0) {
      return { error: 'Ingrese un monto de abono inicial mayor a 0.' };
    }
    if (initial >= totalAmount) {
      return { error: 'El abono inicial debe ser menor al total del cobro.' };
    }
    return { transactions: [{ amount: initial, transactionDate: paymentDate, paymentMethod }] };
  }

  // mode === 'PENDING' => sin transacciones
  return { transactions: undefined };
}

export interface BuildPaymentPayloadParams {
  patientId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  description?: string;
  dueDate?: string;
  appointmentId?: number | null;
  attentionId?: number;
  items: PaymentItem[];
  transactions?: PaymentTransaction[];
}

export function buildPaymentPayload(params: BuildPaymentPayloadParams): Payment {
  return {
    patientId: params.patientId,
    amount: params.amount,
    paymentDate: params.paymentDate,
    paymentMethod: params.paymentMethod,
    description: params.description,
    dueDate: params.dueDate || undefined,
    appointmentId: params.appointmentId ?? undefined,
    attentionId: params.attentionId,
    items: params.items,
    transactions: params.transactions
  };
}
