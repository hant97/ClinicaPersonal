export interface Patient {
  id?: number;
  uuid?: string;
  firstName: string;
  lastName: string;
  documentType?: string;
  identificationDocument: string;
  dateOfBirth?: string;
  contactNumber?: string;
  email?: string;
  occupation?: string;
  maritalStatus?: string;
  emergencyContact?: string;
  reasonForConsultation?: string;
  gender?: string;
  address?: string;
  guardianName?: string;
  guardianContact?: string;
  hasLegalGuardian?: boolean;
  photoUrl?: string;
  active?: boolean;
  specialty?: string;
  hasActiveAlerts?: boolean;
  createdAt?: string;
}

export interface PatientStats {
  totalPatients: number;
  newThisMonth: number;
  withActiveAlerts: number;
  minors: number;
}
