export interface GeneralHistory {
  id?: number;
  patientId: number;
  specialty?: string;
  pathologicalHistory?: string;
  surgicalHistory?: string;
  familyHistory?: string;
  habits?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
