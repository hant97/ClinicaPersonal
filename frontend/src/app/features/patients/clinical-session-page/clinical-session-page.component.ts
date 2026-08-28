import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { COMMON_STANDALONE_IMPORTS } from '../../../shared/common-standalone-imports';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import {
  ArrowLeft,
  Calendar,
  Clock,
  Check,
  X,
  Lock,
  Sparkles,
  AlertCircle,
  AlertTriangle,
  User,
  Phone,
  Mail,
  Pill,
  FileText,
  Stethoscope,
  Brain,
  History,
  Info,
  CalendarDays,
  ShieldCheck,
  Activity
} from '../../../shared/icons/lucide-icons';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { CatalogItem } from '../../../core/models/catalog.model';
import { PageResponse } from '../../../core/models/page.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-clinical-session-page',
  standalone: true,
  imports: [...COMMON_STANDALONE_IMPORTS, RouterModule, LucideAngularModule],
  templateUrl: './clinical-session-page.component.html',
})
export class ClinicalSessionPageComponent implements OnInit, OnDestroy {
  // Iconos
  readonly ArrowLeft = ArrowLeft;
  readonly Calendar = Calendar;
  readonly Clock = Clock;
  readonly Check = Check;
  readonly X = X;
  readonly Lock = Lock;
  readonly Sparkles = Sparkles;
  readonly AlertCircle = AlertCircle;
  readonly AlertTriangle = AlertTriangle;
  readonly User = User;
  readonly Phone = Phone;
  readonly Mail = Mail;
  readonly Pill = Pill;
  readonly FileText = FileText;
  readonly Stethoscope = Stethoscope;
  readonly Brain = Brain;
  readonly History = History;
  readonly Info = Info;
  readonly CalendarDays = CalendarDays;
  readonly ShieldCheck = ShieldCheck;
  readonly Activity = Activity;

  patientIdentifier: string = '';
  sessionId?: number;
  appointmentId?: number;
  appointmentService?: string;

  patient: Patient | null = null;
  clinicalHistory: ClinicalHistory | null = null;
  activeAlerts: RiskAlert[] = [];
  previousSessions: ClinicalSession[] = [];
  lastSession: ClinicalSession | null = null;

  sessionForm!: FormGroup;
  appointmentModalities: CatalogItem[] = [];
  riskLevels: CatalogItem[] = [];
  
  loadingPatient = true;
  loadingHistory = true;
  savingSession = false;
  draftSaved = false;
  isEditing = false;

  private formSubscription?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private clinicalHistoryService: ClinicalHistoryService,
    private riskAlertService: RiskAlertService,
    private catalogService: CatalogService,
    private specialtyService: SpecialtyService,
    private toastService: ToastService,
    private notificationService: NotificationService
  ) {}

  get isPsychology(): boolean {
    return this.specialtyService.isPsychology();
  }

  get isDermatology(): boolean {
    return this.specialtyService.isDermatology();
  }

  /** El paciente tiene una alerta de riesgo activa: se exige completar la evaluación de riesgo estructurada. */
  get requiresRiskAssessment(): boolean {
    return this.isPsychology && this.activeAlerts.length > 0;
  }

  get draftKey(): string {
    return `flowgrid_draft_session_${this.patient?.id || this.patientIdentifier}`;
  }

  get patientAge(): number | null {
    if (!this.patient?.dateOfBirth) return null;
    const dob = new Date(this.patient.dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age;
  }

  ngOnInit(): void {
    this.patientIdentifier = this.route.snapshot.params['id'];
    const sessIdParam = this.route.snapshot.params['sessionId'];
    if (sessIdParam) {
      this.sessionId = Number(sessIdParam);
      this.isEditing = true;
    }

    const qParams = this.route.snapshot.queryParams;
    if (qParams['appointmentId']) {
      this.appointmentId = Number(qParams['appointmentId']);
    }
    if (qParams['service']) {
      this.appointmentService = qParams['service'];
    }

    this.initForm(qParams);
    this.loadCatalogs();
    this.loadPatientAndContext();
  }

  ngOnDestroy(): void {
    this.formSubscription?.unsubscribe();
  }

  @HostListener('window:beforeunload', ['$event'])
  onBeforeUnload(event: BeforeUnloadEvent): void {
    if (this.sessionForm.dirty && !this.savingSession) {
      event.preventDefault();
      event.returnValue = 'Tienes cambios clínicos sin guardar en la consulta.';
    }
  }

  private initForm(qParams: any): void {
    // Determinar valores iniciales (prioridad: queryParams de la cita -> fecha/hora actual)
    let initDate = qParams['date'] || '';
    let initStart = qParams['startTime'] || '';
    let initEnd = qParams['endTime'] || '';
    let initModality = qParams['modality'] || 'PRESENCIAL';

    if (initDate.includes('T')) {
      initDate = initDate.split('T')[0];
    }

    if (!initDate || !initStart) {
      const now = new Date();
      const tzOffset = now.getTimezoneOffset() * 60000;
      const localISO = new Date(now.getTime() - tzOffset).toISOString();
      if (!initDate) initDate = localISO.split('T')[0];
      if (!initStart) {
        const curH = String(now.getHours()).padStart(2, '0');
        const curM = String(now.getMinutes()).padStart(2, '0');
        initStart = `${curH}:${curM}`;
      }
      if (!initEnd) {
        const endMins = now.getHours() * 60 + now.getMinutes() + 45;
        const endH = String(Math.floor(endMins / 60) % 24).padStart(2, '0');
        const endM = String(endMins % 60).padStart(2, '0');
        initEnd = `${endH}:${endM}`;
      }
    } else if (initStart && !initEnd) {
      // Calcular fin a 45 min de inicio si no vino fin
      const [h, m] = initStart.split(':').map(Number);
      const endMins = h * 60 + m + 45;
      const endH = String(Math.floor(endMins / 60) % 24).padStart(2, '0');
      const endM = String(endMins % 60).padStart(2, '0');
      initEnd = `${endH}:${endM}`;
    }

    this.sessionForm = this.fb.group({
      sessionDate: [initDate, [Validators.required]],
      startTime: [initStart.substring(0, 5), [Validators.required]],
      endTime: [initEnd.substring(0, 5), [Validators.required]],
      sessionType: ['INDIVIDUAL', [Validators.required]],
      modality: [initModality, [Validators.required]],
      status: ['COMPLETADA', [Validators.required]],
      subjective: [''],
      objective: [''],
      analysis: [''],
      plan: [''],
      isConfidential: [false],
      riskAssessment: this.fb.group({
        suicidalIdeation: [null],
        ideationFrequency: [''],
        hasPlan: [null],
        planDescription: [''],
        meansAccess: [null],
        meansDescription: [''],
        protectiveFactors: [''],
        riskLevel: [''],
        actionTaken: ['']
      })
    });

    // Escuchar cambios para guardar borrador en localStorage
    this.formSubscription = this.sessionForm.valueChanges.subscribe(val => {
      if (!this.isEditing && this.patient) {
        localStorage.setItem(this.draftKey, JSON.stringify(val));
        this.draftSaved = true;
      }
    });
  }

  private loadCatalogs(): void {
    this.catalogService.getActiveItemsByCatalogCode('APPOINTMENT_MODALITY').subscribe({
      next: (items: CatalogItem[]) => {
        this.appointmentModalities = (items || []).filter(i => i.isActive);
      },
      error: () => {
        this.appointmentModalities = [
          { itemCode: 'PRESENCIAL', itemName: 'Presencial', isActive: true, orderIndex: 0 },
          { itemCode: 'VIRTUAL', itemName: 'Virtual / Videollamada', isActive: true, orderIndex: 1 },
          { itemCode: 'DOMICILIARIA', itemName: 'Atención Domiciliaria', isActive: true, orderIndex: 2 },
        ];
      }
    });

    this.catalogService.getActiveItemsByCatalogCode('RISK_ALERT_LEVEL').subscribe({
      next: (items: CatalogItem[]) => {
        this.riskLevels = (items || []).filter(i => i.isActive);
      },
      error: () => {}
    });
  }

  private loadPatientAndContext(): void {
    this.loadingPatient = true;
    this.patientService.getById(this.patientIdentifier).subscribe({
      next: (patient: Patient) => {
        this.patient = patient;
        this.loadingPatient = false;
        
        // Cargar historial clínico, alertas y sesiones previas
        if (patient.id) {
          this.loadClinicalContext(patient.id);
        }

        // Si estamos editando una sesión existente, cargarla
        if (this.isEditing && this.sessionId) {
          this.loadExistingSession(this.sessionId);
        } else {
          // Restaurar borrador si no se pasaron datos específicos de cita
          this.restoreDraftIfApplicable();
        }
      },
      error: (err: any) => {
        console.error('Error loading patient', err);
        this.loadingPatient = false;
        this.toastService.error('No se pudo cargar la información del paciente');
      }
    });
  }

  private loadClinicalContext(patientId: number): void {
    this.loadingHistory = true;

    // 1. Historial clínico (alergias, medicamentos, diagnósticos)
    this.clinicalHistoryService.get(patientId).subscribe({
      next: (hist: ClinicalHistory) => {
        this.clinicalHistory = hist;
        this.loadingHistory = false;
      },
      error: () => {
        this.loadingHistory = false;
      }
    });

    // 2. Alertas de riesgo
    this.riskAlertService.getAlertsByPatientId(patientId, true).subscribe({
      next: (res: PageResponse<RiskAlert>) => {
        this.activeAlerts = (res.content || []).filter(a => a.active);
      },
      error: () => {}
    });

    // 3. Sesiones previas
    this.sessionService.getSessionsByPatientId(patientId).subscribe({
      next: (res: PageResponse<ClinicalSession>) => {
        this.previousSessions = res.content || [];
        if (this.previousSessions.length > 0) {
          // Tomar la última sesión ordenada por fecha
          this.lastSession = this.previousSessions[0];
        }
      },
      error: () => {}
    });
  }

  private loadExistingSession(sessionId: number): void {
    this.sessionService.getSessionById(sessionId).subscribe({
      next: (sess: ClinicalSession) => {
        let sDate = sess.sessionDate;
        if (sDate.includes('T')) {
          sDate = sDate.split('T')[0];
        }

        let sType = sess.sessionType || 'INDIVIDUAL';
        if (sType === 'Terapia Individual') sType = 'INDIVIDUAL';

        this.sessionForm.patchValue({
          sessionDate: sDate,
          startTime: sess.startTime ? sess.startTime.substring(0, 5) : '',
          endTime: sess.endTime ? sess.endTime.substring(0, 5) : '',
          sessionType: sType,
          modality: sess.modality || 'PRESENCIAL',
          status: sess.status || 'COMPLETADA',
          subjective: sess.subjective || '',
          objective: sess.objective || '',
          analysis: sess.analysis || '',
          plan: sess.plan || '',
          isConfidential: !!(sess.isConfidential || sess.confidential)
        });
        if (sess.riskAssessment) {
          this.sessionForm.get('riskAssessment')?.patchValue(sess.riskAssessment);
        }
        if (sess.appointmentId) {
          this.appointmentId = sess.appointmentId;
        }
      },
      error: (err: any) => {
        console.error('Error loading session', err);
        this.toastService.error('No se pudo cargar la sesión solicitada');
      }
    });
  }

  private restoreDraftIfApplicable(): void {
    if (!this.appointmentId) {
      const savedDraft = localStorage.getItem(this.draftKey);
      if (savedDraft) {
        try {
          const parsed = JSON.parse(savedDraft);
          this.sessionForm.patchValue(parsed, { emitEvent: false });
          this.draftSaved = true;
        } catch {
          // Ignorar borrador corrupto
        }
      }
    }
  }

  // --- Herramientas de Edición Rápida ---

  setDuration(minutes: number): void {
    const start = this.sessionForm.get('startTime')?.value;
    if (!start) return;
    const [h, m] = start.split(':').map(Number);
    const totalMins = h * 60 + m + minutes;
    const endH = String(Math.floor(totalMins / 60) % 24).padStart(2, '0');
    const endM = String(totalMins % 60).padStart(2, '0');
    this.sessionForm.patchValue({ endTime: `${endH}:${endM}` });
  }

  insertSnippet(field: 'subjective' | 'objective' | 'analysis' | 'plan', text: string): void {
    const current = this.sessionForm.get(field)?.value || '';
    const updated = current ? `${current}\n• ${text}: ` : `• ${text}: `;
    this.sessionForm.patchValue({ [field]: updated });
  }

  // --- Guardado y Navegación ---

  async cancel(): Promise<void> {
    if (this.sessionForm.dirty) {
      const confirmed = await this.notificationService.confirm(
        'Salir de la Consulta',
        'Tienes cambios clínicos sin guardar en esta sesión. ¿Seguro que deseas salir?',
        'Sí, salir sin guardar',
        'Continuar redactando'
      );
      if (!confirmed) return;
    }
    this.navigateBackToPatient();
  }

  onSubmit(): void {
    if (this.sessionForm.invalid || !this.patient) {
      this.sessionForm.markAllAsTouched();
      this.toastService.warning('Por favor completa los campos obligatorios marcados con asterisco (*)');
      return;
    }

    const val = this.sessionForm.value;
    const hasAnyContent = (val.subjective || val.objective || val.analysis || val.plan || '').trim().length > 0;
    if (!hasAnyContent) {
      this.toastService.warning('Por favor completa al menos un bloque de evolución clínica (S.O.A.P.)');
      return;
    }

    if (this.requiresRiskAssessment) {
      const ra = val.riskAssessment;
      if (!ra || ra.suicidalIdeation === null || ra.suicidalIdeation === undefined || !ra.riskLevel) {
        this.toastService.warning('El paciente tiene una alerta de riesgo activa: completa la Evaluación de Riesgo Estructurada antes de guardar.');
        return;
      }
    }

    const payload: ClinicalSession = {
      ...val,
      patientId: this.patient.id,
      appointmentId: this.appointmentId || undefined,
      specialty: this.isPsychology ? 'PSICOLOGIA' : 'DERMATOLOGIA',
      riskAssessment: this.isPsychology ? val.riskAssessment : undefined
    };

    this.savingSession = true;

    if (this.isEditing && this.sessionId) {
      this.sessionService.updateSession(this.sessionId, payload).subscribe({
        next: () => {
          this.savingSession = false;
          localStorage.removeItem(this.draftKey);
          this.toastService.success('Consulta clínica actualizada correctamente');
          this.navigateBackToPatient();
        },
        error: (err) => {
          console.error('Error updating session', err);
          this.savingSession = false;
          this.toastService.error(err.error?.message || 'Error al actualizar la consulta');
        }
      });
    } else {
      this.sessionService.createSession(payload).subscribe({
        next: () => {
          this.savingSession = false;
          localStorage.removeItem(this.draftKey);
          this.toastService.success('Consulta clínica registrada exitosamente');
          this.navigateBackToPatient();
        },
        error: (err) => {
          console.error('Error creating session', err);
          this.savingSession = false;
          this.toastService.error(err.error?.message || 'Error al guardar la consulta');
        }
      });
    }
  }

  private navigateBackToPatient(): void {
    this.router.navigate(['/patients', this.patientIdentifier]);
  }
}
