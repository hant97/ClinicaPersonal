export interface ClinicalDocument {
  id?: number;
  patientId: number;
  specialty?: string;
  category?: string;
  name: string;
  mimeType?: string;
  sizeBytes?: number;
  documentDate?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
