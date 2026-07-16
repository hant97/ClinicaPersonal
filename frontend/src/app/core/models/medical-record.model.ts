export interface MedicalRecord {
  id?: number;
  patientId: number;
  diagnosis?: string;
  currentMedication?: string;
  treatmentPlan?: string;
  treatmentStatus?: string;
  createdAt?: string;
  updatedAt?: string;
}
