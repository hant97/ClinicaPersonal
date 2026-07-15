export interface MedicalRecord {
  id?: number;
  patientId: number;
  sessionDate: string;
  reasonForConsultation?: string;
  evolutionNotes?: string;
  presumptiveDiagnosis?: string;
  agreementsAndTasks?: string;
  dynamicData?: string;
}
