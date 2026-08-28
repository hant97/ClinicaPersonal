/**
 * Funciones puras usadas por la vista de detalle de paciente.
 * Extraídas del componente para poder probarlas de forma aislada
 * y reutilizarlas en otras vistas si hiciera falta.
 */

export interface PatientAgeInfo {
  age: number | null;
  esMenorEdad: boolean;
}

export function calculatePatientAge(dateOfBirth?: string | Date | null): PatientAgeInfo {
  if (!dateOfBirth) {
    return { age: null, esMenorEdad: false };
  }
  const birthDate = new Date(dateOfBirth);
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDiff = today.getMonth() - birthDate.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return { age, esMenorEdad: age < 18 };
}

const SESSION_STATUS_BADGE: Record<string, string> = {
  COMPLETADA: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELADA: 'bg-slate-100 text-slate-500 border-slate-200',
  NO_ASISTIO: 'bg-red-50 text-red-700 border-red-200'
};

const SESSION_STATUS_DOT: Record<string, string> = {
  COMPLETADA: 'bg-emerald-500',
  CANCELADA: 'bg-slate-400',
  NO_ASISTIO: 'bg-red-500'
};

export function sessionStatusBadgeClass(status?: string): string {
  return SESSION_STATUS_BADGE[(status || '').toUpperCase()] || 'bg-amber-50 text-amber-700 border-amber-200';
}

export function sessionStatusDotClass(status?: string): string {
  return SESSION_STATUS_DOT[(status || '').toUpperCase()] || 'bg-amber-500';
}

export function getPatientInitials(firstName?: string, lastName?: string): string {
  const f = (firstName || '').charAt(0).toUpperCase();
  const l = (lastName || '').charAt(0).toUpperCase();
  return `${f}${l}` || 'P';
}

export function computeSpecialtyElementsCount(
  counts: Record<string, number>,
  isPsychology: boolean,
  isDermatology: boolean
): number {
  if (isPsychology) {
    return (counts['evaluacion-psicologica'] || 0) +
      (counts['diagnosticos'] || 0) +
      (counts['plan-terapeutico'] || 0);
  }
  if (isDermatology) {
    return (counts['lesiones'] || 0) +
      (counts['examenes-auxiliares'] || 0) +
      (counts['tratamientos'] || 0) +
      (counts['procedimientos'] || 0) +
      (counts['controles'] || 0) +
      (counts['diagnosticos'] || 0);
  }
  return 0;
}

export function computeExpedienteElementsCount(counts: Record<string, number>): number {
  return (counts['alergias'] || 0) +
    (counts['medicamentos'] || 0) +
    (counts['antecedentes-generales'] || 0);
}
