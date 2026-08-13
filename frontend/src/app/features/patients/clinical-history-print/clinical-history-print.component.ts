import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin, Observable, of } from 'rxjs';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { DermatologicalEvaluationService } from '../../../core/services/dermatological-evaluation.service';
import { DermatologicalEvaluation } from '../../../core/models/dermatological-evaluation.model';
import { PageResponse } from '../../../core/models/page.model';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { UserService } from '../../../core/services/user.service';
import { UserProfile } from '../../../core/models/user-profile.model';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { LucideAngularModule, Printer, X } from 'lucide-angular';

@Component({
  selector: 'app-clinical-history-print',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './clinical-history-print.component.html',
  styleUrls: ['./clinical-history-print.component.css'],
})
export class ClinicalHistoryPrintComponent implements OnInit {
  readonly Printer = Printer;
  readonly X = X;

  @Input() patientId!: number;
  @Output() close = new EventEmitter<void>();

  patient: Patient | null = null;
  history: ClinicalHistory | null = null;
  sessions: ClinicalSession[] = [];
  dermatologicalEvaluations: DermatologicalEvaluation[] = [];
  clinicSettings: ClinicSettings | null = null;
  professional: UserProfile | null = null;
  loading = true;

  constructor(
    private clinicalHistoryService: ClinicalHistoryService,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private dermatologicalEvaluationService: DermatologicalEvaluationService,
    private clinicSettingsService: ClinicSettingsService,
    private userService: UserService,
    private specialtyService: SpecialtyService
  ) {}

  get isPsychology(): boolean {
    return this.specialtyService.isPsychology();
  }

  get isDermatology(): boolean {
    return this.specialtyService.isDermatology();
  }

  get fullName(): string {
    if (!this.patient) {
      return '';
    }
    return `${this.patient.firstName} ${this.patient.lastName}`.trim();
  }

  get age(): number | null {
    if (!this.patient?.dateOfBirth) {
      return null;
    }
    const birth = new Date(this.patient.dateOfBirth);
    const today = new Date();
    let result = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      result--;
    }
    return result;
  }

  get professionalName(): string {
    const profile = this.professional;
    if (!profile) {
      return '';
    }
    return `${profile.firstName || ''} ${profile.lastName || ''}`.trim() || profile.username;
  }

  get specialtyLabel(): string {
    return this.isPsychology ? 'Psicología' : 'Dermatología';
  }

  get printDate(): Date {
    return new Date();
  }

  statusLabel(status?: string): string {
    switch (status) {
      case 'ACTIVO':
        return 'Activo';
      case 'RESUELTO':
        return 'Resuelto';
      case 'COMPLETADO':
        return 'Completado';
      case 'SUSPENDIDO':
        return 'Suspendido';
      default:
        return status || '—';
    }
  }

  hasGeneralHistory(): boolean {
    const general = this.history?.generalHistory;
    return !!(
      general &&
      (general.pathologicalHistory ||
        general.surgicalHistory ||
        general.familyHistory ||
        general.habits ||
        general.notes)
    );
  }

  hasDermatologicalHistory(): boolean {
    const derm = this.history?.dermatologicalHistory;
    return !!(
      derm &&
      (derm.skinType ||
        derm.sunExposureHabits ||
        derm.personalSkinHistory ||
        derm.familySkinHistory ||
        derm.chronicConditions ||
        derm.examFindings ||
        derm.notes)
    );
  }

  ngOnInit(): void {
    this.clinicSettingsService.settings$.subscribe((settings) => (this.clinicSettings = settings));
    this.clinicSettingsService.loadSettings();

    const dermatologicalEvaluations$: Observable<PageResponse<DermatologicalEvaluation> | null> =
      this.isDermatology
        ? this.dermatologicalEvaluationService.getByPatientId(this.patientId, 0, 1000)
        : of(null);

    forkJoin({
      patient: this.patientService.getById(this.patientId),
      history: this.clinicalHistoryService.get(this.patientId),
      sessions: this.sessionService.getSessionsByPatientId(this.patientId, 0, 1000),
      professional: this.userService.getCurrentUserProfile(),
      dermatologicalEvaluations: dermatologicalEvaluations$,
    }).subscribe({
      next: (result) => {
        this.patient = result.patient;
        this.history = result.history;
        this.sessions = result.sessions.content;
        this.professional = result.professional;
        this.dermatologicalEvaluations = result.dermatologicalEvaluations?.content ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  getLogoUrl(path: string | undefined): string {
    if (!path) {
      return '';
    }
    return this.clinicSettingsService.getLogoUrl(path);
  }

  print(): void {
    window.print();
  }

  onClose(): void {
    this.close.emit();
  }
}
