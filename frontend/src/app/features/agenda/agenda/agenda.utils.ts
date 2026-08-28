import { Appointment } from '../../../core/models/appointment.model';

/**
 * Funciones puras usadas por la vista de agenda (cálculo de calendario,
 * formato de estados/horas y preparación de datos para exportar).
 * Extraídas del componente para poder probarlas de forma aislada.
 */

export function toDateKey(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function getStartOfWeek(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Lunes
  return new Date(d.setDate(diff));
}

export function isToday(day: Date): boolean {
  const today = new Date();
  return day.getDate() === today.getDate() &&
    day.getMonth() === today.getMonth() &&
    day.getFullYear() === today.getFullYear();
}

export interface AppointmentsDateRangeParams {
  currentView: 'calendar' | 'list';
  weekDays: Date[];
  currentWeekStart: Date;
  listSpecificDate: string | null;
  dateRangeFilter: string;
}

export interface AppointmentsDateRange {
  startDate?: string;
  endDate?: string;
}

/**
 * Reproduce el rango de fechas que debe consultarse al backend según la
 * vista activa (calendario semanal vs. lista) y los filtros seleccionados.
 */
export function computeAppointmentsDateRange(params: AppointmentsDateRangeParams): AppointmentsDateRange {
  const { currentView, weekDays, currentWeekStart, listSpecificDate, dateRangeFilter } = params;

  if (currentView === 'calendar') {
    const start = weekDays[0] || currentWeekStart;
    const end = weekDays[6] || start;
    return { startDate: toDateKey(start), endDate: toDateKey(end) };
  }

  if (listSpecificDate) {
    return { startDate: listSpecificDate, endDate: listSpecificDate };
  }

  if (dateRangeFilter === 'ALL') {
    return {};
  }

  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');

  if (dateRangeFilter === 'TODAY') {
    const todayKey = `${year}-${month}-${day}`;
    return { startDate: todayKey, endDate: todayKey };
  }

  if (dateRangeFilter === 'WEEK') {
    const firstDay = getStartOfWeek(today);
    const lastDay = new Date(firstDay);
    lastDay.setDate(firstDay.getDate() + 6);
    return { startDate: toDateKey(firstDay), endDate: toDateKey(lastDay) };
  }

  if (dateRangeFilter === 'MONTH') {
    const lastDay = new Date(today.getFullYear(), today.getMonth() + 1, 0);
    return { startDate: `${year}-${month}-01`, endDate: `${year}-${month}-${String(lastDay.getDate()).padStart(2, '0')}` };
  }

  return {};
}

export interface TimeSlot {
  startTime: string;
  endTime: string;
}

/** Calcula el rango horario de un slot del calendario a partir de su hora de inicio. */
export function computeSlotTimeRange(hourString: string, durationMinutes = 30): TimeSlot {
  const startTime = hourString.length === 5 ? hourString : hourString.substring(0, 5);
  const [h, min] = startTime.split(':').map(Number);
  const endMinutes = h * 60 + min + durationMinutes;
  const endH = Math.floor(endMinutes / 60);
  const endM = endMinutes % 60;
  const endTime = `${String(endH).padStart(2, '0')}:${String(endM).padStart(2, '0')}`;
  return { startTime, endTime };
}

export function getPatientInitials(name?: string): string {
  if (!name) return 'P';
  const parts = name.trim().split(' ');
  if (parts.length >= 2) {
    return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
  }
  return parts[0].substring(0, 2).toUpperCase();
}

export type StatusPillVariant = 'critical' | 'urgent' | 'priority' | 'stable' | 'neutral';

export function getStatusPillVariant(status: string): StatusPillVariant {
  const s = (status || '').toUpperCase();
  if (s === 'CONFIRMADA') return 'priority';
  if (s === 'PROGRAMADA') return 'urgent';
  if (s === 'COMPLETADA') return 'stable';
  if (s === 'NO_ASISTIO' || s === 'CANCELADA') return 'critical';
  return 'neutral';
}

export function getStatusLabel(status: string): string {
  switch ((status || '').toUpperCase()) {
    case 'PROGRAMADA': return 'Programada';
    case 'CONFIRMADA': return 'Confirmada';
    case 'COMPLETADA': return 'Completada';
    case 'NO_ASISTIO': return 'No asistió';
    case 'CANCELADA': return 'Cancelada';
    default: return status || '';
  }
}

export function getStatusDotClass(status: string): string {
  const s = (status || '').toUpperCase();
  switch (s) {
    case 'PROGRAMADA': return 'bg-amber-500';
    case 'CONFIRMADA': return 'bg-blue-500';
    case 'COMPLETADA': return 'bg-emerald-500';
    case 'CANCELADA': return 'bg-red-500';
    case 'NO_ASISTIO': return 'bg-rose-400';
    default: return 'bg-slate-400';
  }
}

export function formatTimeRange(start?: string, end?: string): string {
  if (!start) return '';
  const s = start.length >= 5 ? start.substring(0, 5) : start;
  const e = end ? (end.length >= 5 ? end.substring(0, 5) : end) : '';
  return e ? `${s} – ${e}` : s;
}

/**
 * Normaliza un número peruano a formato internacional (sin '+') para wa.me.
 * Un celular local se registra con 9 dígitos (ej. 987654321); si ya trae el
 * código de país (51) o es de otro país, se respeta tal cual.
 */
function normalizePeruWhatsAppNumber(phone: string): string {
  const digits = phone.replace(/\D/g, '');
  if (digits.length === 9 && digits.startsWith('9')) {
    return `51${digits}`;
  }
  return digits;
}

export function whatsAppLink(app: Appointment, clinicName?: string): string | null {
  const phone = app.patientPhone;
  if (!phone) return null;
  const digits = normalizePeruWhatsAppNumber(phone);
  if (!digits) return null;

  const firstName = (app.patientName || '').trim().split(/\s+/)[0] || 'estimado(a) paciente';
  const dateLabel = formatReminderDate(app.appointmentDate);
  const timeLabel = (app.startTime || '').substring(0, 5);
  const clinic = clinicName?.trim();

  const message = `Hola ${firstName}, le recordamos su cita`
    + (clinic ? ` en ${clinic}` : '')
    + ` para el ${dateLabel}${timeLabel ? ` a las ${timeLabel}` : ''}. `
    + `Por favor confírmenos su asistencia respondiendo este mensaje. ¡Le esperamos!`;

  return `https://wa.me/${digits}?text=${encodeURIComponent(message)}`;
}

function formatReminderDate(appointmentDate: string): string {
  if (!appointmentDate) return '';
  const datePart = appointmentDate.split('T')[0];
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day) return appointmentDate;
  return `${day}/${month}/${year}`;
}

export function mapAppointmentsForExport(appointments: Appointment[]): Record<string, unknown>[] {
  return appointments.map(app => ({
    'Paciente': app.patientName,
    'Profesional': app.professionalName || '',
    'Fecha': (app.appointmentDate || '').replace('T', ' '),
    'Hora Inicio': app.startTime,
    'Hora Fin': app.endTime,
    'Recurrente': app.recurrenceGroupId ? 'Sí' : 'No',
    'Motivo': app.notes || '',
    'Estado': getStatusLabel(app.status),
    'Tipo': app.modality === 'VIRTUAL' ? 'Virtual' : 'Presencial'
  }));
}
