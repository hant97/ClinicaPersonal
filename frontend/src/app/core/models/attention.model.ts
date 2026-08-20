export type AttentionStatus = 'AGENDADA' | 'EN_PROCESO' | 'ATENDIDA' | 'COBRADA' | 'CANCELADA';

export interface Attention {
  id?: number;
  patientId: number;
  patientName?: string;
  patientDocumentNumber?: string;
  professionalId?: number;
  professionalName?: string;
  appointmentId?: number;
  clinicalSessionId?: number;
  prescriptionId?: number;
  paymentId?: number;
  paymentStatus?: string;
  paymentAmount?: number;
  clinicalServiceId?: number;
  clinicalServiceName?: string;
  specialty?: string;
  attentionDate: string;
  startTime?: string;
  endTime?: string;
  durationMinutes?: number;
  status: AttentionStatus;
  motive?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AttentionSummary {
  totalToday: number;
  scheduledToday: number;
  inProgressToday: number;
  attendedToday: number;
  paidToday: number;
  cancelledToday: number;
  pendingBillingAmount: number;
}

export interface UpdateAttentionStatusRequest {
  status: AttentionStatus;
  notes?: string;
}
