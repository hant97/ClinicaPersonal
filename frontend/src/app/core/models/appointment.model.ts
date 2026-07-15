export interface Appointment {
  id?: number;
  patientId: number;
  appointmentDate: string;
  status: string;
  notes?: string;
  // Para mostrar en UI sin hacer llamadas extra si es necesario:
  patientName?: string;
}
