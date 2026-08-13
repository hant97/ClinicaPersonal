export interface PsychologyEvaluation {
  id?: number;
  patientId: number;
  evaluationDate: string;
  initialEvaluation?: string;
  psychologicalHistory?: string;
  mentalExam?: string;
  notes?: string;
  professionalId?: number;
  createdAt?: string;
  updatedAt?: string;
}
