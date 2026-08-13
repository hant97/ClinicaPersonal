export interface MedicalRecord {
  id?: number;
  patientId: number;
  diagnosis?: string;
  currentMedication?: string;
  treatmentPlan?: string;
  treatmentStatus?: string;
  specialty?: string;
  skinType?: string;
  knownAllergies?: string;
  chronicConditions?: string;
  sunExposureHabits?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
