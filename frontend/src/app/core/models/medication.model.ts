export interface Medication {
  id?: number;
  patientId: number;
  specialty?: string;
  name: string;
  dose?: string;
  frequency?: string;
  startDate?: string;
  endDate?: string;
  active: boolean;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
