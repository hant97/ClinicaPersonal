export interface PrescriptionItem {
  id?: number;
  name: string;
  dose?: string;
  frequency?: string;
  duration?: string;
  route?: string;
  instructions?: string;
}

export interface Prescription {
  id?: number;
  patientId: number;
  specialty?: string;
  prescriptionDate?: string;
  validUntil?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
  items: PrescriptionItem[];
}
