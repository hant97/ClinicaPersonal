import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PatientDetailDataService } from './patient-detail-data.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { Appointment } from '../../../core/models/appointment.model';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { AssessmentListComponent } from '../assessment-list/assessment-list.component';
import { RiskAlertFormComponent } from '../risk-alert-form/risk-alert-form.component';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { Allergy } from '../../../core/models/allergy.model';
import { Medication } from '../../../core/models/medication.model';
import { Diagnosis } from '../../../core/models/diagnosis.model';
import { GeneralHistorySectionComponent } from '../general-history-section/general-history-section.component';
import { AllergiesSectionComponent } from '../allergies-section/allergies-section.component';
import { MedicationsSectionComponent } from '../medications-section/medications-section.component';
import { PrescriptionsSectionComponent } from '../prescriptions-section/prescriptions-section.component';
import { DocumentsSectionComponent } from '../documents-section/documents-section.component';
import { RiskAlertBannerComponent } from '../risk-alert-banner/risk-alert-banner.component';
import { PsychologyEvaluationSectionComponent } from '../psychology-evaluation-section/psychology-evaluation-section.component';
import { DiagnosesSectionComponent } from '../diagnoses-section/diagnoses-section.component';
import { TherapeuticPlansSectionComponent } from '../therapeutic-plans-section/therapeutic-plans-section.component';
import { DermatologicalHistorySectionComponent } from '../dermatological-history-section/dermatological-history-section.component';
import { LesionsSectionComponent } from '../lesions-section/lesions-section.component';
import { AuxiliaryExamsSectionComponent } from '../auxiliary-exams-section/auxiliary-exams-section.component';
import { TreatmentsSectionComponent } from '../treatments-section/treatments-section.component';
import { ProceduresSectionComponent } from '../procedures-section/procedures-section.component';
import { EvolutionsSectionComponent } from '../evolutions-section/evolutions-section.component';
import { DermatologicalEvaluationListComponent } from '../dermatological-evaluation-list/dermatological-evaluation-list.component';
import { ClinicalHistoryPrintComponent } from '../clinical-history-print/clinical-history-print.component';
import { PatientPaymentsSectionComponent } from '../patient-payments-section/patient-payments-section.component';
import { PatientSummaryHeaderComponent } from '../patient-summary-header/patient-summary-header.component';
import { OnboardingService } from '../../../shared/services/onboarding/onboarding.service';
import {
  calculatePatientAge,
  sessionStatusBadgeClass,
  sessionStatusDotClass,
  getPatientInitials,
  computeSpecialtyElementsCount,
  computeExpedienteElementsCount
} from './patient-detail.utils';
import { LucideAngularModule } from 'lucide-angular';
import {
  FileText,
  Pill,
  ClipboardList,
  Activity,
  Calendar,
  User,
  Phone,
  AlertTriangle,
  Brain,
  Stethoscope,
  Microscope,
  Printer,
  Plus,
  ArrowLeft,
  Mail,
  Clock,
  Edit,
  Trash2,
  CheckCircle2,
  FolderOpen,
  Sparkles,
  ChevronRight,
  ChevronDown,
  ChevronUp,
  Receipt,
  CalendarCheck,
  ImagePlus,
  Layers,
  HeartPulse
} from '../../../shared/icons/lucide-icons';

export type MainTabType = 'timeline' | 'expediente' | 'especialidad' | 'cobros';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    CommonModule,
    AssessmentListComponent,
    RiskAlertFormComponent,
    GeneralHistorySectionComponent,
    AllergiesSectionComponent,
    MedicationsSectionComponent,
    PrescriptionsSectionComponent,
    DocumentsSectionComponent,
    PsychologyEvaluationSectionComponent,
    DiagnosesSectionComponent,
    TherapeuticPlansSectionComponent,
    DermatologicalHistorySectionComponent,
    LesionsSectionComponent,
    AuxiliaryExamsSectionComponent,
    TreatmentsSectionComponent,
    ProceduresSectionComponent,
    EvolutionsSectionComponent,
    DermatologicalEvaluationListComponent,
    ClinicalHistoryPrintComponent,
    PatientPaymentsSectionComponent,
    RiskAlertBannerComponent,
    PatientSummaryHeaderComponent,
    LucideAngularModule
  ],
  templateUrl: './patient-detail.component.html',
})
export class PatientDetailComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  readonly FileText = FileText;
  readonly Pill = Pill;
  readonly ClipboardList = ClipboardList;
  readonly User = User;
  readonly Phone = Phone;
  readonly Activity = Activity;
  readonly Calendar = Calendar;
  readonly Printer = Printer;
  readonly Plus = Plus;
  readonly ArrowLeft = ArrowLeft;
  readonly Mail = Mail;
  readonly Clock = Clock;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly CheckCircle2 = CheckCircle2;
  readonly FolderOpen = FolderOpen;
  readonly Sparkles = Sparkles;
  readonly Brain = Brain;
  readonly Stethoscope = Stethoscope;
  readonly Microscope = Microscope;
  readonly AlertTriangle = AlertTriangle;
  readonly ChevronRight = ChevronRight;
  readonly ChevronDown = ChevronDown;
  readonly ChevronUp = ChevronUp;
  readonly Receipt = Receipt;
  readonly CalendarCheck = CalendarCheck;
  readonly ImagePlus = ImagePlus;
  readonly Layers = Layers;
  readonly HeartPulse = HeartPulse;

  patient: Patient | null = null;
  sessions: ClinicalSession[] = [];
  upcomingAppointments: Appointment[] = [];
  recentAppointments: Appointment[] = [];
  selectedSession?: ClinicalSession;
  showForm = false;
  /** Datos de la cita de origen (agenda/dashboard) pendientes de aplicar a la próxima sesión nueva que se abra. */
  pendingAppointmentContext: { appointmentId?: number; date?: string; startTime?: string; endTime?: string } | null = null;
  showAlertForm = false;
  showPrint = false;
  expandedSessionId: number | null = null;
  showAlertsModal = false;
  showContact = false;
  paymentsCount = 0;
  activeAlerts: RiskAlert[] = [];
  esMenorEdad = false;
  age: number | null = null;

  // FlowGrid 360 Workspace Tabs
  activeTab: MainTabType = 'timeline';
  activeSpecialtySubTab = 'evaluacion-inicial';
  activeExpedienteSubTab = 'todo';
  collapsedSections: Record<string, boolean> = {};
  showFirstVisitBanner = false;

  counts: Record<string, number> = {};
  allergies: Allergy[] = [];
  medications: Medication[] = [];
  diagnoses: Diagnosis[] = [];

  isPsychology = false;
  isDermatology = false;
  specialtyElementsCount = 0;
  expedienteElementsCount = 0;
  nextUpcomingAppointment?: Appointment;

  patientId: number | null = null;
  currentIdentifier: string = '';
  isLoading = true;
  loadError = false;
  errorMessage = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dataService: PatientDetailDataService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private specialtyService: SpecialtyService,
    private onboardingService: OnboardingService
  ) {}

  ngOnInit(): void {
    this.isPsychology = this.specialtyService.isPsychology();
    this.isDermatology = this.specialtyService.isDermatology();

    if (this.isPsychology) {
      this.activeSpecialtySubTab = 'evaluacion-psicologica';
    } else if (this.isDermatology) {
      this.activeSpecialtySubTab = 'evaluacion-inicial';
    }

    // Primera vez que el médico abre una ficha de paciente: arranca solo
    // "Datos Personales" expandido, con un banner que explica el patrón.
    if (!this.onboardingService.isFirstPatientVisitSeen()) {
      this.showFirstVisitBanner = true;
      this.collapsedSections = {
        'antecedentes': true,
        'alergias': true,
        'medicamentos': true,
        'recetas': true,
        'documentos': true
      };
    }

    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadAllPatientData(id);
      }
    });

    this.route.queryParams.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      if (params['newSession'] === 'true') {
        this.activeTab = 'timeline';
        this.pendingAppointmentContext = {
          appointmentId: params['appointmentId'] ? Number(params['appointmentId']) : undefined,
          date: params['date'] || undefined,
          startTime: params['startTime'] || undefined,
          endTime: params['endTime'] || undefined
        };
        this.openForm();
      }
      if (params['tab'] === 'prescriptions') {
        this.activeTab = 'expediente';
        this.activeExpedienteSubTab = 'recetas';
        this.collapsedSections['recetas'] = false;
      }
    });
  }

  loadAllPatientData(identifier: string): void {
    this.currentIdentifier = identifier;
    this.isLoading = true;
    this.loadError = false;
    this.errorMessage = '';
    this.loadPatient(identifier);
  }

  setTab(tab: MainTabType): void {
    this.activeTab = tab;
  }

  setSpecialtySubTab(subTab: string): void {
    this.activeSpecialtySubTab = subTab;
  }

  setExpedienteSubTab(subTab: string): void {
    this.activeExpedienteSubTab = subTab;
  }

  openAlertsModal(): void {
    this.showAlertsModal = true;
  }

  openAllergiesSection(): void {
    this.activeTab = 'expediente';
    this.activeExpedienteSubTab = 'alergias';
  }

  openMedicationsSection(): void {
    this.activeTab = 'expediente';
    this.activeExpedienteSubTab = 'medicamentos';
  }

  openDiagnosesSection(): void {
    this.activeTab = 'especialidad';
    this.activeSpecialtySubTab = 'diagnosticos';
  }

  closeAlertsModal(): void {
    this.showAlertsModal = false;
  }

  toggleContact(): void {
    this.showContact = !this.showContact;
  }

  dismissFirstVisitBanner(): void {
    this.showFirstVisitBanner = false;
    this.onboardingService.markFirstPatientVisitSeen();
  }

  isSectionCollapsed(section: string): boolean {
    return !!this.collapsedSections[section];
  }

  toggleSectionCollapse(section: string): void {
    this.collapsedSections[section] = !this.collapsedSections[section];
  }

  expandAllSections(): void {
    this.collapsedSections = {};
  }

  collapseAllSections(): void {
    this.collapsedSections = {
      'datos-personales': true,
      'antecedentes': true,
      'alergias': true,
      'medicamentos': true,
      'recetas': true,
      'documentos': true
    };
  }

  sessionStatusBadge(status?: string): string {
    return sessionStatusBadgeClass(status);
  }

  sessionStatusDot(status?: string): string {
    return sessionStatusDotClass(status);
  }

  loadCounts(patientId: number): void {
    this.dataService.loadHistorySummary(patientId).subscribe({
      next: ({ history, counts }) => {
        this.allergies = history.allergies || [];
        this.medications = history.medications || [];
        this.diagnoses = history.diagnoses || [];
        this.counts = counts;
        this.specialtyElementsCount = computeSpecialtyElementsCount(counts, this.isPsychology, this.isDermatology);
        this.expedienteElementsCount = computeExpedienteElementsCount(counts);
      },
      error: () => this.toastService.show('No se pudo cargar la historia clínica', 'error')
    });
  }

  loadAlerts(patientId: number): void {
    this.dataService.loadActiveAlertsWithLabels(patientId, this.isPsychology).subscribe({
      next: (alerts) => this.activeAlerts = alerts,
      error: () => this.toastService.show('No se pudieron resolver las alertas del paciente', 'error')
    });
  }

  openAlertForm(): void {
    this.showAlertForm = true;
  }

  closeAlertForm(): void {
    this.showAlertForm = false;
  }

  onAlertSaved(): void {
    this.closeAlertForm();
    if (this.patient?.id) {
      this.loadAlerts(this.patient.id);
    }
  }

  async resolveAlert(alertId: number, event: Event): Promise<void> {
    event.stopPropagation();
    const confirmed = await this.notificationService.confirm(
      'Resolver Alerta',
      '¿Está seguro de marcar esta alerta como resuelta?',
      'Sí, resolver',
      'Cancelar'
    );
    if (confirmed && this.patient?.id) {
      this.dataService.resolveAlert(this.patient.id, alertId).subscribe({
        next: () => {
          this.toastService.show('Alerta resuelta', 'success');
          this.loadAlerts(this.patient!.id!);
        },
        error: () => this.toastService.show('Error al resolver alerta', 'error')
      });
    }
  }

  loadPatient(identifier: string | number): void {
    this.isLoading = true;
    this.loadError = false;
    this.errorMessage = '';
    this.dataService.loadPatient(identifier).subscribe({
      next: (data) => {
        this.patient = data;
        this.patientId = data.id || null;
        this.isLoading = false;
        this.calculateEsMenorEdad();
        if (data.id) {
          this.loadSessions(data.id);
          this.loadAlerts(data.id);
          this.loadCounts(data.id);
          this.loadAppointments(data.id);
        }
      },
      error: (err) => {
        this.patient = null;
        this.isLoading = false;
        this.loadError = true;
        this.errorMessage = err.status === 404
          ? 'El paciente no fue encontrado o no pertenece a la especialidad activa.'
          : 'No se pudo cargar la información del paciente. Por favor, intenta de nuevo.';
        console.error('Error fetching patient', err);
      }
    });
  }

  calculateEsMenorEdad(): void {
    const { age, esMenorEdad } = calculatePatientAge(this.patient?.dateOfBirth);
    this.age = age;
    this.esMenorEdad = esMenorEdad;
  }

  loadSessions(patientId: number): void {
    this.dataService.loadSessions(patientId).subscribe({
      next: (sessions) => this.sessions = sessions,
      error: () => this.toastService.show('No se pudieron cargar las sesiones clínicas', 'error')
    });
  }

  loadAppointments(patientId: number): void {
    this.dataService.loadAppointmentsSummary(patientId).subscribe({
      next: ({ upcoming, recent, next }) => {
        this.upcomingAppointments = upcoming;
        this.recentAppointments = recent;
        this.nextUpcomingAppointment = next;
      },
      error: () => this.toastService.show('No se pudieron cargar las citas del paciente', 'error')
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0 && this.patient?.id) {
      this.dataService.uploadPatientPhoto(this.patient.id, input.files[0]).subscribe({
        next: (updated) => {
          this.patient = updated;
          this.toastService.show('Foto actualizada', 'success');
        },
        error: () => this.toastService.show('Error al subir la foto', 'error')
      });
    }
    input.value = '';
  }

  openForm(session?: ClinicalSession): void {
    const key = this.patient?.uuid || this.patient?.id || this.currentIdentifier;
    if (session?.id) {
      this.router.navigate(['/patients', key, 'sessions', session.id, 'edit']);
    } else {
      const queryParams = this.pendingAppointmentContext || undefined;
      this.pendingAppointmentContext = null;
      this.router.navigate(['/patients', key, 'sessions', 'new'], queryParams ? { queryParams } : undefined);
    }
  }

  closeForm(): void {
    this.showForm = false;
    this.selectedSession = undefined;
  }

  onSessionSaved(): void {
    this.closeForm();
    if (this.patient?.id) {
      this.loadSessions(this.patient.id);
    }
  }

  goBack(): void {
    this.router.navigate(['/patients']);
  }

  toggleSessionExpand(sessionId: number): void {
    this.expandedSessionId = this.expandedSessionId === sessionId ? null : sessionId;
  }

  async deleteSession(id: number, event: Event): Promise<void> {
    event.stopPropagation();
    const confirmed = await this.notificationService.confirm(
      'Eliminar Sesión',
      '¿Está seguro de que desea eliminar esta sesión? Esta acción no se puede deshacer.',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.dataService.deleteSession(id).subscribe({
        next: () => {
          this.toastService.show('Sesión eliminada exitosamente', 'success');
          if (this.patient?.id) {
            this.loadSessions(this.patient.id);
          }
        },
        error: (err) => {
          console.error('Error deleting session', err);
          this.toastService.show('Error al eliminar la sesión', 'error');
        }
      });
    }
  }

  editSession(id: number, event: Event): void {
    event.stopPropagation();
    const key = this.patient?.uuid || this.patient?.id || this.currentIdentifier;
    this.router.navigate(['/patients', key, 'sessions', id, 'edit']);
  }

  scheduleAppointment(): void {
    if (this.patient?.id) {
      this.router.navigate(['/agenda'], {
        queryParams: { newAppointment: 'true', patientId: this.patient.id }
      });
    }
  }

  openPrint(): void {
    this.showPrint = true;
  }

  closePrint(): void {
    this.showPrint = false;
  }

  getInitials(firstName?: string, lastName?: string): string {
    return getPatientInitials(firstName, lastName);
  }
}
