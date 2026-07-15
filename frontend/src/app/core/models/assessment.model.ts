export interface Option {
  score: number;
  text: string;
}

export interface Question {
  id: number;
  text: string;
  options: Option[];
}

export interface PsychometricTest {
  id: number;
  name: string;
  description: string;
  questionsJson: string; // JSON string of Question[]
}

export interface Assessment {
  id?: number;
  patientId: number;
  psychometricTestId: number;
  testName?: string;
  assessmentDate?: string;
  totalScore: number;
  answersJson: string; // JSON string like '{"1": 2, "2": 0}'
  notes?: string;
}
