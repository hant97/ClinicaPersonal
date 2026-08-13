export interface Procedure {
  id?: number;
  patientId: number;
  name: string;
  description?: string;
  procedureDate?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
