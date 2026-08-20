export type PaymentStatus = 'PENDIENTE' | 'PARCIAL' | 'PAGADO';

export interface PaymentItem {
  id?: number;
  paymentId?: number;
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  supplyId?: number;
  clinicalServiceId?: number;
}

export interface PaymentTransaction {
  id?: number;
  paymentId?: number;
  amount: number;
  transactionDate?: string;
  paymentMethod?: string;
  notes?: string;
}

export interface Payment {
  id?: number;
  patientId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  description?: string;
  appointmentId?: number;
  attentionId?: number;
  clinicalSessionId?: number;
  dueDate?: string;
  // Solo lectura (calculados en el servidor)
  status?: PaymentStatus;
  paidAmount?: number;
  balanceAmount?: number;
  // Para UI
  patientName?: string;
  items?: PaymentItem[];
  transactions?: PaymentTransaction[];
}

export interface PatientBalance {
  patientId: number;
  totalCharged: number;
  totalPaid: number;
  balance: number;
}

export interface PaymentMethodSummary {
  method: string;
  total: number;
  count: number;
}

export interface DailyIncome {
  date: string;
  total: number;
}

export interface ServiceSummary {
  name: string;
  quantity: number;
  total: number;
}

export interface PaymentSummary {
  incomeToday: number;
  incomeMonth: number;
  monthlyGrowth: number;
  paymentsCountMonth: number;
  averageTicket: number;
  pendingBalance: number;
  methodBreakdown: PaymentMethodSummary[];
  dailyIncome: DailyIncome[];
  topServices: ServiceSummary[];
}
