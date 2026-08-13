import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
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
import { RouterLink } from '@angular/router';
import {
  LucideAngularModule,
  LucideIconData,
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
} from 'lucide-angular';

interface NavItem {
  id: string;
  label: string;
  icon: LucideIconData;
}

interface NavGroup {
  title: string;
  items: NavItem[];
}

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ClinicalSessionFormComponent, AssessmentListComponent, RiskAlertFormComponent, GeneralHistorySectionComponent, AllergiesSectionComponent, MedicationsSectionComponent, PsychologyEvaluationSectionComponent, DiagnosesSectionComponent, TherapeuticPlansSectionComponent, DermatologicalHistorySectionComponent, LesionsSectionComponent, AuxiliaryExamsSectionComponent, TreatmentsSectionComponent, ProceduresSectionComponent, EvolutionsSectionComponent, DermatologicalEvaluationListComponent, ClinicalHistoryPrintComponent, LucideAngularModule],
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

  patient: Patient | null = null;
  sessions: ClinicalSession[] = [];
  activeAlerts: RiskAlert[] = [];
  showForm = false;
  showAlertForm = false;
  showPrint = false;
  expandedSessionId: number | null = null;
  selectedSession: ClinicalSession | undefined;
  esMenorEdad = false;
  age: number | null = null;

  navGroups: NavGroup[] = [];
  activeSection = 'datos-personales';
  counts: Record<string, number> = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
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
    this.buildNav();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPatient(Number(id));
      this.loadSessions(Number(id));
      this.loadAlerts(Number(id));
      this.loadCounts(Number(id));
    }

    // Fase 2: Abrir sesión automáticamente si viene de completar cita
    this.route.queryParams.subscribe(params => {
      if (params['newSession'] === 'true') {
        this.activeSection = 'sesiones';
        this.openForm();
      }
    });
  }

  private buildNav(): void {
    const groups: NavGroup[] = [
      {
        title: 'Paciente',
        items: [{ id: 'datos-personales', label: 'Datos personales', icon: User }],
      },
      {
        title: 'General',
        items: [
          { id: 'antecedentes-generales', label: 'Antecedentes generales', icon: FileText },
          { id: 'alergias', label: 'Alergias', icon: AlertTriangle },
          { id: 'medicamentos', label: 'Medicamentos', icon: Pill },
        ],
      },
    ];

    if (this.isPsychology) {
      groups.push({
        title: 'Psicología',
        items: [
          { id: 'evaluacion-psicologica', label: 'Evaluación inicial', icon: Brain },
          { id: 'pruebas-psicologicas', label: 'Pruebas psicológicas', icon: ClipboardList },
          { id: 'diagnosticos', label: 'Diagnósticos', icon: Stethoscope },
          { id: 'plan-terapeutico', label: 'Plan terapéutico', icon: ClipboardList },
          { id: 'sesiones', label: 'Sesiones / Evoluciones', icon: Calendar },
        ],
      });
    }

    if (this.isDermatology) {
      groups.push({
        title: 'Dermatología',
        items: [
          { id: 'evaluacion-inicial', label: 'Evaluación inicial', icon: Stethoscope },
          { id: 'antecedentes-dermatologicos', label: 'Antecedentes dermatológicos', icon: FileText },
          { id: 'lesiones', label: 'Lesiones', icon: Activity },
          { id: 'diagnosticos', label: 'Diagnósticos', icon: Stethoscope },
          { id: 'examenes-auxiliares', label: 'Exámenes auxiliares', icon: Microscope },
          { id: 'tratamientos', label: 'Tratamientos', icon: Pill },
          { id: 'procedimientos', label: 'Procedimientos', icon: Activity },
          { id: 'controles', label: 'Controles / Evoluciones', icon: Calendar },
          { id: 'sesiones', label: 'Sesiones', icon: Calendar },
        ],
      });
    }

    groups.push({
      title: 'Otros',
      items: [
        { id: 'alertas', label: 'Alertas de riesgo', icon: AlertTriangle },
      ],
    });

    this.navGroups = groups;
  }

  selectSection(id: string): void {
    this.activeSection = id;
    if (this.patient?.id) {
      this.loadCounts(this.patient.id);
    }
  }

  loadCounts(patientId: number): void {
    this.clinicalHistoryService.get(patientId).subscribe({
      next: (history: ClinicalHistory) => {
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

  countFor(id: string): number {
    if (id === 'sesiones') {
      return this.sessions.length;
    }
    if (id === 'alertas') {
      return this.activeAlerts.length;
    }
    return this.counts[id] ?? 0;
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
            // Filter alerts that belong to this specialty and map their labels
            const validTypes = new Set(types.map(t => t.itemCode));
            const filteredAlerts: RiskAlert[] = [];

            this.activeAlerts.forEach(alert => {
              // MockDataSeeder uses names directly instead of codes for legacy data,
              // we check if it matches either the code or the name.
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
    if (this.expandedSessionId === sessionId) {
      this.expandedSessionId = null;
    } else {
      this.expandedSessionId = sessionId;
    }
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

  openPrint(): void {
    this.showPrint = true;
  }

  closePrint(): void {
    this.showPrint = false;
  }
}
