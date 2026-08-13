import { GeneralHistory } from './general-history.model';
import { Allergy } from './allergy.model';
import { Medication } from './medication.model';
import { Diagnosis } from './diagnosis.model';
import { PsychologyEvaluation } from './psychology-evaluation.model';
import { TherapeuticPlan } from './therapeutic-plan.model';
import { DermatologicalHistory } from './dermatological-history.model';
import { Lesion } from './lesion.model';
import { AuxiliaryExam } from './auxiliary-exam.model';
import { Treatment } from './treatment.model';
import { Procedure } from './procedure.model';
import { Evolution } from './evolution.model';

export interface ClinicalHistory {
  patientId: number;
  specialty: string;
  generalHistory?: GeneralHistory;
  allergies: Allergy[];
  medications: Medication[];
  diagnoses: Diagnosis[];
  psychologyEvaluations?: PsychologyEvaluation[];
  therapeuticPlans?: TherapeuticPlan[];
  dermatologicalHistory?: DermatologicalHistory;
  lesions?: Lesion[];
  auxiliaryExams?: AuxiliaryExam[];
  treatments?: Treatment[];
  procedures?: Procedure[];
  evolutions?: Evolution[];
}
