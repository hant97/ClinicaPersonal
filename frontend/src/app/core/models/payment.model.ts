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

export interface Payment {
  id?: number;
  patientId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  description?: string;
  appointmentId?: number;
  // Para UI
  patientName?: string;
  items?: PaymentItem[];
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
  methodBreakdown: PaymentMethodSummary[];
  dailyIncome: DailyIncome[];
  topServices: ServiceSummary[];
}
