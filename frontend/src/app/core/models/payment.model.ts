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
  // Para UI
  patientName?: string;
  items?: PaymentItem[];
}
