export interface DermatologicalHistory {
  id?: number;
  patientId: number;
  skinType?: string;
  sunExposureHabits?: string;
  personalSkinHistory?: string;
  familySkinHistory?: string;
  chronicConditions?: string;
  examFindings?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
