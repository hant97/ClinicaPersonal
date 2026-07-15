export interface Payment {
  id?: number;
  patientId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  description?: string;
  // Para UI
  patientName?: string;
}
