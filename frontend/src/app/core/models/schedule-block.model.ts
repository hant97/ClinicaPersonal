export interface ScheduleBlock {
  id?: number;
  professionalId?: number;
  professionalName?: string;
  specialty?: string;
  title: string;
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  startTime?: string; // HH:mm
  endTime?: string; // HH:mm
  reason?: string;
}
