export interface Appointment {
  id?: number;
  patientId: number;
  appointmentDate: string;
  startTime: string;
  endTime: string;
  status: string; // PROGRAMADA, CONFIRMADA, COMPLETADA, CANCELADA, NO_ASISTIO
  modality?: string; // PRESENCIAL, VIRTUAL
  videoCallLink?: string;
  professionalId?: number;
  isFirstTime: boolean;
  clinicalSessionId?: number;
  notes?: string;
  
  // Para mostrar en UI sin hacer llamadas extra si es necesario:
  patientName?: string;
}
