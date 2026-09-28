import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Appointment } from '../../../core/models/appointment.model';
import { Diagnosis } from '../../../core/models/diagnosis.model';
import { Medication } from '../../../core/models/medication.model';
import { Patient } from '../../../core/models/patient.model';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { AuthImageSrcDirective } from '../../../shared/directives/auth-image-src.directive';
import {
  AlertTriangle, CalendarCheck, ChevronDown, ChevronRight, ImagePlus,
  Mail, Phone, Pill, Stethoscope
} from '../../../shared/icons/lucide-icons';
import { getPatientInitials } from './patient-detail.utils';

@Component({
  selector: 'app-patient-clinical-sidebar',
  standalone: true,
  imports: [CommonModule, AuthImageSrcDirective, LucideAngularModule],
  host: { class: 'block sticky top-20' },
  templateUrl: './patient-clinical-sidebar.component.html'
})
export class PatientClinicalSidebarComponent {
  readonly AlertTriangle = AlertTriangle;
  readonly CalendarCheck = CalendarCheck;
  readonly ChevronDown = ChevronDown;
  readonly ChevronRight = ChevronRight;
  readonly ImagePlus = ImagePlus;
  readonly Mail = Mail;
  readonly Phone = Phone;
  readonly Pill = Pill;
  readonly Stethoscope = Stethoscope;

  @Input({ required: true }) patient!: Patient;
  @Input() age: number | null = null;
  @Input() isProfessional = false;
  @Input() upcomingAppointments: Appointment[] = [];
  @Input() recentAppointments: Appointment[] = [];
  @Input() activeAlerts: RiskAlert[] = [];
  @Input() diagnoses: Diagnosis[] = [];
  @Input() medications: Medication[] = [];

  @Output() photoSelected = new EventEmitter<Event>();
  @Output() alertsRequested = new EventEmitter<void>();

  showContact = false;

  toggleContact(): void {
    this.showContact = !this.showContact;
  }

  getInitials(firstName?: string, lastName?: string): string {
    return getPatientInitials(firstName, lastName);
  }
}
