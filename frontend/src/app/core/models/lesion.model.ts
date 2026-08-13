export interface Lesion {
  id?: number;
  patientId: number;
  bodyArea?: string;
  lesionType?: string;
  size?: string;
  morphology?: string;
  color?: string;
  sinceDate?: string;
  evolution?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
