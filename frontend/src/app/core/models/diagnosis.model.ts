export interface Diagnosis {
  id?: number;
  patientId: number;
  specialty?: string;
  category?: string;
  description: string;
  status?: string;
  diagnosisDate?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
