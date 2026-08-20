export interface Appointment {
  id?: number;
  patientId: number;
  patientUuid?: string;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: string; // PROGRAMADA, CONFIRMADA, COMPLETADA, CANCELADA, NO_ASISTIO
  modality?: string; // PRESENCIAL, VIRTUAL
  videoCallLink?: string;
  professionalId?: number;
  isFirstTime?: boolean;
  firstTime?: boolean;
  clinicalSessionId?: number;
  clinicalServiceId?: number;
  clinicalServiceName?: string;
  notes?: string;
  
  // Para mostrar en UI sin hacer llamadas extra si es necesario:
  patientName?: string;
  isPaid?: boolean;
  paid?: boolean;
  paymentId?: number;
  paymentAmount?: number;
  googleEventId?: string;
  googleEventLink?: string;
  reminderSentAt?: string;
  confirmedAt?: string;
  patientEmail?: string;
  patientPhone?: string;
  professionalName?: string;
  recurrenceGroupId?: string;
  recurrenceRule?: string;
  recurrenceCount?: number;
}

export interface PublicAppointmentConfirmation {
  confirmed: boolean;
  alreadyConfirmed: boolean;
  message: string;
  patientName?: string;
  appointmentDate?: string;
  startTime?: string;
  modality?: string;
  clinicName?: string;
}

