export interface Patient {
  id?: number;
  firstName: string;
  lastName: string;
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
  createdAt?: string;
}
