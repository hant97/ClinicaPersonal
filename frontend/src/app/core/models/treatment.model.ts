export interface Treatment {
  id?: number;
  patientId: number;
  name: string;
  dose?: string;
  route?: string;
  frequency?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
