export interface AuxiliaryExam {
  id?: number;
  patientId: number;
  examType?: string;
  description?: string;
  result?: string;
  examDate?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
