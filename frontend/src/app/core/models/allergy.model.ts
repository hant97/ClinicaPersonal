export interface Allergy {
  id?: number;
  patientId: number;
  specialty?: string;
  allergen: string;
  type?: string;
  severity?: string;
  reaction?: string;
  active: boolean;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
