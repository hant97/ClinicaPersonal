import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';
import { Appointment } from '../../../core/models/appointment.model';
import { Diagnosis } from '../../../core/models/diagnosis.model';
import { Medication } from '../../../core/models/medication.model';
import { Patient } from '../../../core/models/patient.model';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { AlertTriangle, CalendarCheck, Pill, Stethoscope } from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-patient-clinical-sidebar',
  standalone: true,
  imports: [CommonModule, LucideDynamicIcon],
  host: { class: 'block sticky top-20' },
  templateUrl: './patient-clinical-sidebar.component.html'
})
export class PatientClinicalSidebarComponent {
  readonly AlertTriangle = AlertTriangle;
  readonly CalendarCheck = CalendarCheck;
  readonly Pill = Pill;
  readonly Stethoscope = Stethoscope;

  @Input({ required: true }) patient!: Patient;
  @Input() isProfessional = false;
  @Input() upcomingAppointments: Appointment[] = [];
  @Input() recentAppointments: Appointment[] = [];
  @Input() activeAlerts: RiskAlert[] = [];
  @Input() diagnoses: Diagnosis[] = [];
  @Input() medications: Medication[] = [];

  @Output() alertsRequested = new EventEmitter<void>();

  appointmentDateLabel(isoDate: string): string {
    const appointmentDate = this.parseLocalDate(isoDate);
    if (!appointmentDate) return isoDate;

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    if (appointmentDate.getTime() === today.getTime()) return 'Hoy';

    const tomorrow = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 1);
    if (appointmentDate.getTime() === tomorrow.getTime()) return 'Mañana';

    return appointmentDate.toLocaleDateString('es-PE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  appointmentStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PROGRAMADA: 'Programada',
      CONFIRMADA: 'Confirmada',
      COMPLETADA: 'Completada',
      CANCELADA: 'Cancelada',
      NO_ASISTIO: 'No asistió'
    };
    const normalizedStatus = (status || '').toUpperCase();
    if (labels[normalizedStatus]) return labels[normalizedStatus];
    if (!status) return 'Sin estado';

    const readableStatus = status.replace(/_/g, ' ').toLocaleLowerCase('es-PE');
    return readableStatus.charAt(0).toLocaleUpperCase('es-PE') + readableStatus.slice(1);
  }

  private parseLocalDate(isoDate: string): Date | null {
    const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(isoDate);
    if (!match) return null;

    const [, year, month, day] = match;
    const date = new Date(Number(year), Number(month) - 1, Number(day));
    return date.getFullYear() === Number(year) &&
      date.getMonth() === Number(month) - 1 &&
      date.getDate() === Number(day)
      ? date
      : null;
  }
}
