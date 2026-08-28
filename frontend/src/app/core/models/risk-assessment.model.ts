export interface RiskAssessment {
  id?: number;
  patientId?: number;
  clinicalSessionId?: number;
  professionalId?: number;
  suicidalIdeation: boolean | null;
  ideationFrequency?: string;
  hasPlan?: boolean | null;
  planDescription?: string;
  meansAccess?: boolean | null;
  meansDescription?: string;
  protectiveFactors?: string;
  riskLevel: string;
  actionTaken?: string;
  createdAt?: string;
  updatedAt?: string;
}
