import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import { ArrowLeft, Calendar, Edit, ImagePlus,
 Mail, Phone, Plus, Printer } from '../../../shared/icons/lucide-icons';
import { Patient } from '../../../core/models/patient.model';
import { AuthImageSrcDirective } from '../../../shared/directives/auth-image-src.directive';

@Component({
  selector: 'app-patient-summary-header',
  standalone: true,
  imports: [CommonModule, RouterLink, LucideAngularModule, AuthImageSrcDirective],
  templateUrl: './patient-summary-header.component.html'
})
export class PatientSummaryHeaderComponent {
  @Input({ required: true }) patient!: Patient;
  @Input() age: number | null = null;
  @Input() isMinor = false;
  /** Falso para usuarios sin rol de profesional: se ocultan las acciones clínicas. */
  @Input() clinicalAccess = true;

  @Output() backRequested = new EventEmitter<void>();
  @Output() printRequested = new EventEmitter<void>();
  @Output() appointmentRequested = new EventEmitter<void>();
  @Output() sessionRequested = new EventEmitter<void>();
  @Output() photoSelected = new EventEmitter<Event>();

  readonly ArrowLeft = ArrowLeft;
  readonly Calendar = Calendar;
  readonly Edit = Edit;
  readonly ImagePlus = ImagePlus;
  readonly Mail = Mail;
  readonly Phone = Phone;
  readonly Plus = Plus;
  readonly Printer = Printer;

  get initials(): string {
    return `${this.patient.firstName?.charAt(0) ?? ''}${this.patient.lastName?.charAt(0) ?? ''}`.toUpperCase();
  }
}
