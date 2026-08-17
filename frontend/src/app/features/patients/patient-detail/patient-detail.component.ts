import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { PaymentService } from '../../../core/services/payment.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { Payment } from '../../../core/models/payment.model';
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
  Receipt,
  Wallet,
  CalendarCheck,
  ImagePlus
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
  readonly Brain = Brain;
  readonly Stethoscope = Stethoscope;
  readonly Microscope = Microscope;
  readonly AlertTriangle = AlertTriangle;
  readonly Sparkles = Sparkles;
  readonly ChevronRight = ChevronRight;
  readonly ChevronDown = ChevronDown;
  readonly Receipt = Receipt;
  readonly Wallet = Wallet;
  readonly CalendarCheck = CalendarCheck;
  readonly ImagePlus = ImagePlus;

  patient: Patient | null = null;
  sessions: ClinicalSession[] = [];
  activeAlerts: RiskAlert[] = [];
  payments: Payment[] = [];
  paymentsTotal = 0;
  upcomingAppointments: Appointment[] = [];
  recentAppointments: Appointment[] = [];
  paymentMethodMap = new Map<string, string>();
  showForm = false;
  showAlertForm = false;
  showPrint = false;
  showAlertsModal = false;
  showContact = false;
  expandedSessionId: number | null = null;
  selectedSession: ClinicalSession | undefined;
  esMenorEdad = false;
  age: number | null = null;

  // FlowGrid 360 Workspace Tabs
  activeTab: MainTabType = 'timeline';
  activeSpecialtySubTab = 'evaluacion-inicial';
  activeExpedienteSubTab = 'datos-personales';

  counts: Record<string, number> = {};
  allergies: any[] = [];
  medications: any[] = [];
  diagnoses: any[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private paymentService: PaymentService,
    private appointmentService: AppointmentService,
    private riskAlertService: RiskAlertService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private specialtyService: SpecialtyService,
    private clinicalHistoryService: ClinicalHistoryService
  ) {}

  get isPsychology(): boolean {
    return this.specialtyService.isPsychology();
  }

  get isDermatology(): boolean {
    return this.specialtyService.isDermatology();
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPatient(Number(id));
      this.loadSessions(Number(id));
      this.loadAlerts(Number(id));
      this.loadCounts(Number(id));
      this.loadPayments(Number(id));
      this.loadAppointments(Number(id));
    }

    this.catalogService.getActiveItemsByCatalogCode('PAYMENT_METHOD').subscribe({
      next: (items) => items.forEach(item => this.paymentMethodMap.set(item.itemCode, item.itemName))
    });

    if (this.isPsychology) {
      this.activeSpecialtySubTab = 'evaluacion-psicologica';
    } else if (this.isDermatology) {
      this.activeSpecialtySubTab = 'evaluacion-inicial';
    }

    this.route.queryParams.subscribe(params => {
      if (params['newSession'] === 'true') {
        this.activeTab = 'timeline';
        this.openForm();
      }
    });
  }

  setTab(tab: MainTabType): void {
    this.activeTab = tab;
    if (this.patient?.id) {
      this.loadCounts(this.patient.id);
    }
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

  closeAlertsModal(): void {
    this.showAlertsModal = false;
  }

  toggleContact(): void {
    this.showContact = !this.showContact;
  }

  sessionStatusBadge(status?: string): string {
    switch ((status || '').toUpperCase()) {
      case 'COMPLETADA':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'CANCELADA':
        return 'bg-slate-100 text-slate-500 border-slate-200';
      case 'NO_ASISTIO':
        return 'bg-red-50 text-red-700 border-red-200';
      default:
        return 'bg-amber-50 text-amber-700 border-amber-200';
    }
  }

  sessionStatusDot(status?: string): string {
    switch ((status || '').toUpperCase()) {
      case 'COMPLETADA':
        return 'bg-emerald-500';
      case 'CANCELADA':
        return 'bg-slate-400';
      case 'NO_ASISTIO':
        return 'bg-red-500';
      default:
        return 'bg-amber-500';
    }
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

  loadPatient(id: number): void {
    this.patientService.getById(id).subscribe({
      next: (data) => {
        this.patient = data;
        this.calculateEsMenorEdad();
      },
      error: (err) => console.error('Error fetching patient', err)
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

  loadPayments(patientId: number): void {
    this.paymentService.getByPatientId(patientId, 0, 100).subscribe({
      next: (page) => {
        this.payments = page.content;
        this.paymentsTotal = page.content.reduce((sum, p) => sum + (p.amount || 0), 0);
      },
      error: (err) => console.error('Error fetching payments', err)
    });
  }

  loadAppointments(patientId: number): void {
    this.appointmentService.getByPatientId(patientId, 0, 200).subscribe({
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
      },
      error: (err) => console.error('Error fetching appointments', err)
    });
  }

  getPaymentMethodText(method: string): string {
    return this.paymentMethodMap.get(method) || method;
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
    this.selectedSession = session;
    this.showForm = true;
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
    const session = this.sessions.find(s => s.id === id);
    if (session) {
      this.openForm(session);
    }
  }

  scheduleAppointment(): void {
    if (this.patient?.id) {
      this.router.navigate(['/agenda'], {
        queryParams: { newAppointment: 'true', patientId: this.patient.id }
      });
    }
  }

  registerPayment(): void {
    if (this.patient?.id) {
      this.router.navigate(['/billing'], {
        queryParams: { newPayment: 'true', patientId: this.patient.id }
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
