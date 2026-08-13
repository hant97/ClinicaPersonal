export interface TherapeuticPlan {
  id?: number;
  patientId: number;
  specialty?: string;
  objectives?: string;
  interventions?: string;
  startDate?: string;
  endDate?: string;
  status?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
