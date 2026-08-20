import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { Appointment } from '../../../core/models/appointment.model';
import { ClinicalSessionFormComponent } from '../clinical-session-form/clinical-session-form.component';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { AssessmentListComponent } from '../assessment-list/assessment-list.component';
import { RiskAlertFormComponent } from '../risk-alert-form/risk-alert-form.component';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';
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
import { StatusPillComponent } from '../../../shared/components/status-pill/status-pill.component';
import {
  LucideAngularModule,
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
} from 'lucide-angular';

export type MainTabType = 'timeline' | 'expediente' | 'especialidad' | 'cobros';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ClinicalSessionFormComponent,
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
    LucideAngularModule,
    StatusPillComponent
  ],
  templateUrl: './patient-detail.component.html',
})
export class PatientDetailComponent implements OnInit {
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

  counts: Record<string, number> = {};
  allergies: any[] = [];
  medications: any[] = [];
  diagnoses: any[] = [];

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
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private appointmentService: AppointmentService,
    private riskAlertService: RiskAlertService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private specialtyService: SpecialtyService,
    private clinicalHistoryService: ClinicalHistoryService
  ) {}

  ngOnInit(): void {
    this.isPsychology = this.specialtyService.isPsychology();
    this.isDermatology = this.specialtyService.isDermatology();

    if (this.isPsychology) {
      this.activeSpecialtySubTab = 'evaluacion-psicologica';
    } else if (this.isDermatology) {
      this.activeSpecialtySubTab = 'evaluacion-inicial';
    }

    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadAllPatientData(id);
      }
    });

    this.route.queryParams.subscribe(params => {
      if (params['newSession'] === 'true') {
        this.activeTab = 'timeline';
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

  private updateComputedCounts(): void {
    if (this.isPsychology) {
      this.specialtyElementsCount = (this.counts['evaluacion-psicologica'] || 0) +
                                    (this.counts['diagnosticos'] || 0) +
                                    (this.counts['plan-terapeutico'] || 0);
    } else if (this.isDermatology) {
      this.specialtyElementsCount = (this.counts['lesiones'] || 0) +
                                    (this.counts['examenes-auxiliares'] || 0) +
                                    (this.counts['tratamientos'] || 0) +
                                    (this.counts['procedimientos'] || 0) +
                                    (this.counts['controles'] || 0) +
                                    (this.counts['diagnosticos'] || 0);
    } else {
      this.specialtyElementsCount = 0;
    }

    this.expedienteElementsCount = (this.counts['alergias'] || 0) +
                                   (this.counts['medicamentos'] || 0) +
                                   (this.counts['antecedentes-generales'] || 0);
  }

  private readonly badgeMap: Record<string, string> = {
    COMPLETADA: 'bg-emerald-50 text-emerald-700 border-emerald-200',
    CANCELADA: 'bg-slate-100 text-slate-500 border-slate-200',
    NO_ASISTIO: 'bg-red-50 text-red-700 border-red-200'
  };

  private readonly dotMap: Record<string, string> = {
    COMPLETADA: 'bg-emerald-500',
    CANCELADA: 'bg-slate-400',
    NO_ASISTIO: 'bg-red-500'
  };

  sessionStatusBadge(status?: string): string {
    return this.badgeMap[(status || '').toUpperCase()] || 'bg-amber-50 text-amber-700 border-amber-200';
  }

  sessionStatusDot(status?: string): string {
    return this.dotMap[(status || '').toUpperCase()] || 'bg-amber-500';
  }

  loadCounts(patientId: number): void {
    this.clinicalHistoryService.get(patientId).subscribe({
      next: (history: ClinicalHistory) => {
        this.allergies = history.allergies || [];
        this.medications = history.medications || [];
        this.diagnoses = history.diagnoses || [];
        this.counts = {
          'antecedentes-generales': history.generalHistory?.id ? 1 : 0,
          'alergias': history.allergies?.length ?? 0,
          'medicamentos': history.medications?.length ?? 0,
          'diagnosticos': history.diagnoses?.length ?? 0,
          'evaluacion-psicologica': history.psychologyEvaluations?.length ?? 0,
          'plan-terapeutico': history.therapeuticPlans?.length ?? 0,
          'antecedentes-dermatologicos': history.dermatologicalHistory?.id ? 1 : 0,
          'lesiones': history.lesions?.length ?? 0,
          'examenes-auxiliares': history.auxiliaryExams?.length ?? 0,
          'tratamientos': history.treatments?.length ?? 0,
          'procedimientos': history.procedures?.length ?? 0,
          'controles': history.evolutions?.length ?? 0
        };
        this.updateComputedCounts();
      },
      error: () => {}
    });
  }

  loadAlerts(patientId: number): void {
    this.riskAlertService.getAlertsByPatientId(patientId, true).subscribe({
      next: (data) => {
        this.activeAlerts = data.content;
        this.resolveAlertLabels();
      },
      error: (err) => console.error('Error fetching alerts', err)
    });
  }

  resolveAlertLabels(): void {
    const catalogCode = this.isPsychology ? 'RISK_ALERT_TYPE' : 'RISK_ALERT_TYPE_DERM';
    this.catalogService.getActiveItemsByCatalogCode(catalogCode).subscribe({
      next: (types) => {
        this.catalogService.getActiveItemsByCatalogCode('RISK_ALERT_LEVEL').subscribe({
          next: (levels) => {
            const validTypes = new Set(types.map(t => t.itemCode));
            const filteredAlerts: RiskAlert[] = [];

            this.activeAlerts.forEach(alert => {
              const typeItem = types.find(t => t.itemCode === alert.type || t.itemName === alert.type);

              if (typeItem) {
                const levelItem = levels.find(l => l.itemCode === alert.level || l.itemName === alert.level);
                alert.type = typeItem.itemName;
                alert.level = levelItem ? levelItem.itemName : alert.level;
                filteredAlerts.push(alert);
              }
            });

            this.activeAlerts = filteredAlerts;
          },
          error: (err) => console.error('Error fetching RISK_ALERT_LEVEL', err)
        });
      },
      error: (err) => console.error('Error fetching ' + catalogCode, err)
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
      this.riskAlertService.resolveAlert(this.patient.id, alertId).subscribe({
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
    this.patientService.getById(identifier).subscribe({
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
    if (this.patient?.dateOfBirth) {
      const birthDate = new Date(this.patient.dateOfBirth);
      const today = new Date();
      let age = today.getFullYear() - birthDate.getFullYear();
      const m = today.getMonth() - birthDate.getMonth();
      if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
        age--;
      }
      this.age = age;
      this.esMenorEdad = age < 18;
    } else {
      this.age = null;
      this.esMenorEdad = false;
    }
  }

  loadSessions(patientId: number): void {
    this.sessionService.getSessionsByPatientId(patientId).subscribe({
      next: (data) => this.sessions = data.content,
      error: (err) => console.error('Error fetching sessions', err)
    });
  }

  loadAppointments(patientId: number): void {
    this.appointmentService.getByPatientId(patientId, 0, 10).subscribe({
      next: (page) => {
        const today = new Date().toISOString().split('T')[0];
        const all = page.content;
        this.upcomingAppointments = all
          .filter(a => a.appointmentDate >= today && a.status !== 'CANCELADA' && a.status !== 'NO_ASISTIO' && a.status !== 'COMPLETADA')
          .sort((a, b) => a.appointmentDate.localeCompare(b.appointmentDate));
        this.recentAppointments = all
          .filter(a => a.appointmentDate < today || a.status === 'COMPLETADA')
          .sort((a, b) => b.appointmentDate.localeCompare(a.appointmentDate))
          .slice(0, 3);
        this.nextUpcomingAppointment = this.upcomingAppointments.length > 0 ? this.upcomingAppointments[0] : undefined;
      },
      error: (err) => console.error('Error fetching appointments', err)
    });
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0 && this.patient?.id) {
      this.patientService.uploadPhoto(this.patient.id, input.files[0]).subscribe({
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
      this.router.navigate(['/patients', key, 'sessions', 'new']);
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
      this.sessionService.deleteSession(id).subscribe({
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
    const f = (firstName || '').charAt(0).toUpperCase();
    const l = (lastName || '').charAt(0).toUpperCase();
    return `${f}${l}` || 'P';
  }
}
