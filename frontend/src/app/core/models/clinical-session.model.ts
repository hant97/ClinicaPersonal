export interface ClinicalSession {
  id?: number;
  patientId: number;
  sessionDate: string;
  startTime: string;
  endTime: string;
  sessionType: string;
  modality: string;
  status: string;
  subjective?: string;
  objective?: string;
  analysis?: string;
  plan?: string;
  isConfidential: boolean;
  createdAt?: string;
  updatedAt?: string;
  professionalId?: number;
  appointmentId?: number;
}
