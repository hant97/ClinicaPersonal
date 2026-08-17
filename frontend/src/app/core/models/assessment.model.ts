export interface Option {
  score: number;
  text: string;
}

export interface Question {
  id: number;
  text: string;
  options: Option[];
}

export interface InterpretationBand {
  minScore: number;
  maxScore: number;
  label: string;
  color: string;
  description: string;
}

export interface PsychometricTest {
  id: number;
  name: string;
  description: string;
  questionsJson: string; // JSON string of Question[]
  interpretationJson?: string | null; // JSON string of InterpretationBand[]
  usageCount?: number;
  lastUsedAt?: string;
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
  interpretationJson?: string | null; // JSON string of InterpretationBand[]
}

export interface BandColor {
  key: string;
  label: string;
}

export const BAND_COLOR_PALETTE: BandColor[] = [
  { key: 'emerald', label: 'Verde' },
  { key: 'blue', label: 'Azul' },
  { key: 'amber', label: 'Ámbar' },
  { key: 'orange', label: 'Naranja' },
  { key: 'red', label: 'Rojo' },
  { key: 'violet', label: 'Violeta' },
  { key: 'slate', label: 'Gris' }
];

export function bandColorClasses(color?: string): { badge: string; dot: string } {
  switch (color) {
    case 'emerald':
      return { badge: 'bg-emerald-50 text-emerald-700 border-emerald-200', dot: 'bg-emerald-500' };
    case 'blue':
      return { badge: 'bg-blue-50 text-blue-700 border-blue-200', dot: 'bg-blue-500' };
    case 'amber':
      return { badge: 'bg-amber-50 text-amber-700 border-amber-200', dot: 'bg-amber-500' };
    case 'orange':
      return { badge: 'bg-orange-50 text-orange-700 border-orange-200', dot: 'bg-orange-500' };
    case 'red':
      return { badge: 'bg-red-50 text-red-700 border-red-200', dot: 'bg-red-500' };
    case 'violet':
      return { badge: 'bg-violet-50 text-violet-700 border-violet-200', dot: 'bg-violet-500' };
    default:
      return { badge: 'bg-slate-100 text-slate-700 border-line', dot: 'bg-slate-400' };
  }
}

export function parseBands(json?: string | null): InterpretationBand[] {
  if (!json) {
    return [];
  }
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function interpretScore(json: string | null | undefined, score: number | undefined | null): InterpretationBand | null {
  if (score == null) {
    return null;
  }
  return parseBands(json).find(band => score >= band.minScore && score <= band.maxScore) ?? null;
}
