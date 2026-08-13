export interface DermatologicalEvaluation {
  id?: number;
  patientId: number;
  evaluationDate: string;
  skinType?: string;
  affectedArea?: string;
  lesionType?: string;
  lesionSize?: string;
  dermatologicalDiagnosis?: string;
  treatmentIndicated?: string;
  procedurePerformed?: string;
  evolutionNotes?: string;
  nextReviewDate?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
