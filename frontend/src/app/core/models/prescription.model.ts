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
  verificationCode?: string;
  createdAt?: string;
  updatedAt?: string;
  items: PrescriptionItem[];
}

export interface PublicPrescriptionVerification {
  verificationCode: string;
  patientName: string;
  patientIdentificationDocument?: string;
  prescriptionDate: string;
  validUntil?: string;
  valid: boolean;
  statusMessage: string;
  professionalName: string;
  specialty: string;
  clinicName: string;
  notes?: string;
  items: PrescriptionItem[];
}
