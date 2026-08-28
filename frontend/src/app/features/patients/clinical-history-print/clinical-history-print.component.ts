import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable, of } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { DermatologicalEvaluationService } from '../../../core/services/dermatological-evaluation.service';
import { DermatologicalEvaluation } from '../../../core/models/dermatological-evaluation.model';
import { fetchAllPages } from '../../../core/utils/pagination.util';
import { ClinicSettingsService, ClinicSettings } from '../../../core/services/clinic-settings.service';
import { UserService } from '../../../core/services/user.service';
import { UserProfile } from '../../../core/models/user-profile.model';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  Printer,
  X,
  FileText,
  Check,
  Filter,
  Calendar,
  AlertTriangle,
  Lock,
  User,
  Layers,
  Settings2,
  ChevronDown,
  ChevronUp,
  MapPin,
  Phone,
  Mail,
  ShieldCheck,
  Building2,
  Stethoscope,
  RotateCw
} from '../../../shared/icons/lucide-icons';

export type PrintMode = 'completa' | 'resumen' | 'ultima' | 'personalizada';
export type SessionFilterType = 'all' | 'last1' | 'last3' | 'last5' | 'custom';

export interface PrintSectionsConfig {
  patientInfo: boolean;
  alertsBanner: boolean;
  generalHistory: boolean;
  allergies: boolean;
  medications: boolean;
  diagnoses: boolean;
  specialtyModule: boolean;
  sessions: boolean;
  signatureBlock: boolean;
}

@Component({
  selector: 'app-clinical-history-print',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './clinical-history-print.component.html',
  styleUrls: ['./clinical-history-print.component.css'],
})
export class ClinicalHistoryPrintComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  readonly Printer = Printer;
  readonly X = X;
  readonly FileText = FileText;
  readonly Check = Check;
  readonly Filter = Filter;
  readonly Calendar = Calendar;
  readonly AlertTriangle = AlertTriangle;
  readonly Lock = Lock;
  readonly User = User;
  readonly Layers = Layers;
  readonly Settings2 = Settings2;
  readonly ChevronDown = ChevronDown;
  readonly ChevronUp = ChevronUp;
  readonly MapPin = MapPin;
  readonly Phone = Phone;
  readonly Mail = Mail;
  readonly ShieldCheck = ShieldCheck;
  readonly Building2 = Building2;
  readonly Stethoscope = Stethoscope;
  readonly RotateCw = RotateCw;

  @Input() patientId!: number;
  @Output() close = new EventEmitter<void>();

  patient: Patient | null = null;
  history: ClinicalHistory | null = null;
  sessions: ClinicalSession[] = [];
  dermatologicalEvaluations: DermatologicalEvaluation[] = [];
  clinicSettings: ClinicSettings | null = null;
  professional: UserProfile | null = null;
  loading = true;
  loadError = false;

  // Print Configuration State
  showConfigPanel = false;
  printMode: PrintMode = 'completa';
  sessionFilter: SessionFilterType = 'all';
  sessionDateFrom = '';
  sessionDateTo = '';
  excludeConfidential = true;

  sections: PrintSectionsConfig = {
    patientInfo: true,
    alertsBanner: true,
    generalHistory: true,
    allergies: true,
    medications: true,
    diagnoses: true,
    specialtyModule: true,
    sessions: true,
    signatureBlock: true,
  };

  constructor(
    private clinicalHistoryService: ClinicalHistoryService,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private dermatologicalEvaluationService: DermatologicalEvaluationService,
    private clinicSettingsService: ClinicSettingsService,
    private userService: UserService,
    private specialtyService: SpecialtyService,
    private toastService: ToastService
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

  get clinicDisplayName(): string {
    const name = this.clinicSettings?.clinicName || this.clinicSettings?.shortName;
    if (name && name !== 'Cargando...' && name !== 'Clínica' && name !== '...') {
      return name;
    }
    return 'CENTRO MÉDICO VIDA SALUDABLE';
  }

  get documentIdFormatted(): string {
    return `HC-${this.patientId.toString().padStart(6, '0')}`;
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

  setPrintMode(mode: PrintMode): void {
    this.printMode = mode;
    switch (mode) {
      case 'completa':
        this.sections = {
          patientInfo: true,
          alertsBanner: true,
          generalHistory: true,
          allergies: true,
          medications: true,
          diagnoses: true,
          specialtyModule: true,
          sessions: true,
          signatureBlock: true,
        };
        this.sessionFilter = 'all';
        break;

      case 'resumen':
        this.sections = {
          patientInfo: true,
          alertsBanner: true,
          generalHistory: true,
          allergies: true,
          medications: true,
          diagnoses: true,
          specialtyModule: false,
          sessions: false,
          signatureBlock: true,
        };
        this.sessionFilter = 'all';
        break;

      case 'ultima':
        this.sections = {
          patientInfo: true,
          alertsBanner: true,
          generalHistory: false,
          allergies: true,
          medications: true,
          diagnoses: true,
          specialtyModule: false,
          sessions: true,
          signatureBlock: true,
        };
        this.sessionFilter = 'last1';
        break;

      case 'personalizada':
        // Mantiene la selección actual
        break;
    }
  }

  toggleConfigPanel(): void {
    this.showConfigPanel = !this.showConfigPanel;
  }

  get filteredSessions(): ClinicalSession[] {
    let list = [...this.sessions];

    if (this.excludeConfidential) {
      list = list.filter(s => !s.isConfidential);
    }

    list.sort((a, b) => b.sessionDate.localeCompare(a.sessionDate));

    if (this.sessionFilter === 'custom') {
      if (this.sessionDateFrom) {
        list = list.filter(s => s.sessionDate >= this.sessionDateFrom);
      }
      if (this.sessionDateTo) {
        list = list.filter(s => s.sessionDate <= this.sessionDateTo);
      }
      return list;
    }

    switch (this.sessionFilter) {
      case 'last1':
        return list.slice(0, 1);
      case 'last3':
        return list.slice(0, 3);
      case 'last5':
        return list.slice(0, 5);
      case 'all':
      default:
        return list;
    }
  }

  get activeAllergiesList(): any[] {
    return (this.history?.allergies || []).filter(a => a.active);
  }

  get hasSevereAllergy(): boolean {
    return this.activeAllergiesList.some(a => (a.severity || '').toLowerCase() === 'grave' || (a.severity || '').toLowerCase() === 'severa');
  }

  get activeMedicationsList(): any[] {
    return (this.history?.medications || []).filter(m => m.active);
  }

  get activeDiagnosesList(): any[] {
    return (this.history?.diagnoses || []).filter(d => (d.status || '').toUpperCase() === 'ACTIVO');
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
    this.clinicSettingsService.settings$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((settings) => (this.clinicSettings = settings));
    this.clinicSettingsService.loadSettings();
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.loadError = false;

    const dermatologicalEvaluations$: Observable<DermatologicalEvaluation[] | null> =
      this.isDermatology
        ? fetchAllPages((page, size) =>
            this.dermatologicalEvaluationService.getByPatientId(this.patientId, page, size)
          )
        : of(null);

    forkJoin({
      patient: this.patientService.getById(this.patientId),
      history: this.clinicalHistoryService.get(this.patientId),
      sessions: fetchAllPages((page, size) =>
        this.sessionService.getSessionsByPatientId(this.patientId, page, size)
      ),
      professional: this.userService.getCurrentUserProfile(),
      dermatologicalEvaluations: dermatologicalEvaluations$,
    }).subscribe({
      next: (result) => {
        this.patient = result.patient;
        this.history = result.history;
        this.sessions = result.sessions;
        this.professional = result.professional;
        this.dermatologicalEvaluations = result.dermatologicalEvaluations ?? [];
        this.loading = false;
        this.loadError = false;
      },
      error: (err) => {
        console.error('Error al cargar datos de historia clínica para impresión', err);
        this.loading = false;
        this.loadError = true;
        this.toastService.show('Error al cargar datos para impresión. Intente nuevamente.', 'error');
      },
    });
  }

  retryLoad(): void {
    this.loadData();
  }

  getLogoUrl(path: string | undefined): string {
    if (!path) {
      return '';
    }
    return this.clinicSettingsService.getLogoUrl(path);
  }

  print(): void {
    const originalTitle = document.title;
    document.title = `Historia_Clinica_${this.documentIdFormatted}_${this.fullName.replace(/\s+/g, '_')}`;
    window.print();
    setTimeout(() => {
      document.title = originalTitle;
    }, 1000);
  }


  onClose(): void {
    this.close.emit();
  }
}
