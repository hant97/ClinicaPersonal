export interface RiskAlert {
  id?: number;
  patientId: number;
  patientName?: string;
  type: string;
  level: string;
  description?: string;
  active: boolean;
  resolvedAt?: string;
  createdAt?: string;
}
