export interface ProfessionalSchedule {
  id?: number;
  professionalId?: number;
  dayOfWeek: number; // 1 = Lunes, ..., 7 = Domingo
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  active: boolean;
  specialty?: string;
}

export interface WeeklySchedule {
  professionalId: number;
  professionalName?: string;
  specialty?: string;
  schedules: ProfessionalSchedule[];
}
