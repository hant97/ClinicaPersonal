export interface Evolution {
  id?: number;
  patientId: number;
  controlDate: string;
  clinicalNotes?: string;
  nextControlDate?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
