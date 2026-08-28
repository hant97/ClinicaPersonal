import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Allergy } from '../../../core/models/allergy.model';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { Appointment } from '../../../core/models/appointment.model';
import { LucideAngularModule } from 'lucide-angular';
import {
  AlertTriangle,
  Pill,
  Stethoscope,
  CalendarCheck,
  CheckCircle2,
  ShieldAlert,
  ChevronRight,
  Sparkles,
  Calendar
} from '../../../shared/icons/lucide-icons';

@Component({
  selector: 'app-risk-alert-banner',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './risk-alert-banner.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RiskAlertBannerComponent implements OnChanges {
  readonly AlertTriangle = AlertTriangle;
  readonly Pill = Pill;
  readonly Stethoscope = Stethoscope;
  readonly CalendarCheck = CalendarCheck;
  readonly CheckCircle2 = CheckCircle2;
  readonly ShieldAlert = ShieldAlert;
  readonly ChevronRight = ChevronRight;
  readonly Sparkles = Sparkles;
  readonly Calendar = Calendar;

  @Input() allergies: Allergy[] = [];
  @Input() activeAlerts: RiskAlert[] = [];
  @Input() medications: any[] = [];
  @Input() diagnoses: any[] = [];
  @Input() upcomingAppointment?: Appointment;

  @Output() openAllergies = new EventEmitter<void>();
  @Output() openAlerts = new EventEmitter<void>();
  @Output() openMedications = new EventEmitter<void>();
  @Output() openDiagnoses = new EventEmitter<void>();
  @Output() scheduleAppointment = new EventEmitter<void>();

  activeAllergiesList: Allergy[] = [];
  hasSevereAllergy = false;
  allergiesSummary = 'Sin alergias';

  hasHighRiskAlert = false;

  activeMedicationsList: any[] = [];
  medicationsSummary = 'Sin medicación';

  activeDiagnosesList: any[] = [];
  primaryDiagnosis: any | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['allergies']) {
      this.activeAllergiesList = (this.allergies || []).filter(a => a.active !== false);
      this.hasSevereAllergy = this.activeAllergiesList.some(
        a => (a.severity || '').toLowerCase() === 'grave' || (a.severity || '').toLowerCase() === 'severa'
      );
      if (this.activeAllergiesList.length === 0) {
        this.allergiesSummary = 'Sin alergias';
      } else {
        const items = this.activeAllergiesList.map(a => `${a.allergen}${a.severity ? ` (${a.severity})` : ''}`);
        this.allergiesSummary = items.length <= 2 ? items.join(' · ') : `${items.slice(0, 2).join(' · ')} +${items.length - 2} más`;
      }
    }

    if (changes['activeAlerts']) {
      this.hasHighRiskAlert = (this.activeAlerts || []).some(a => (a.level || '').toLowerCase() === 'alto');
    }

    if (changes['medications']) {
      this.activeMedicationsList = (this.medications || []).filter(m => m.active !== false);
      if (this.activeMedicationsList.length === 0) {
        this.medicationsSummary = 'Sin medicación';
      } else {
        const items = this.activeMedicationsList.map(m => m.name || m.medicationName || 'Medicamento');
        this.medicationsSummary = items.length <= 2 ? items.join(', ') : `${items.slice(0, 2).join(', ')} +${items.length - 2} más`;
      }
    }

    if (changes['diagnoses']) {
      this.activeDiagnosesList = (this.diagnoses || []).filter(d => (d.status || '').toUpperCase() !== 'RESUELTO');
      this.primaryDiagnosis = this.activeDiagnosesList.length > 0 ? this.activeDiagnosesList[0] : null;
    }
  }
}

