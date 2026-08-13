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
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { MedicalRecord } from '../../../core/models/medical-record.model';
import { MedicalRecordFormComponent } from '../medical-record-form/medical-record-form.component';
import { RouterLink } from '@angular/router';
import { LucideAngularModule, FileText, Pill, ClipboardList, Activity, Calendar, User, Phone } from 'lucide-angular';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, ClinicalSessionFormComponent, AssessmentListComponent, RiskAlertFormComponent, MedicalRecordFormComponent, LucideAngularModule],
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

  patient: Patient | null = null;
  sessions: ClinicalSession[] = [];
  activeAlerts: RiskAlert[] = [];
  medicalRecords: MedicalRecord[] = [];
  showForm = false;
  showAlertForm = false;
  showMedicalRecordForm = false;
  expandedSessionId: number | null = null;
  selectedSession: ClinicalSession | undefined;
  selectedMedicalRecord: MedicalRecord | undefined;
  esMenorEdad = false;
  age: number | null = null;
  showPersonalDetails = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private riskAlertService: RiskAlertService,
    private medicalRecordService: MedicalRecordService,
    private catalogService: CatalogService,
    private notificationService: NotificationService,
    private toastService: ToastService,
    private specialtyService: SpecialtyService
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
      this.loadMedicalRecords(Number(id));
    }

    // Fase 2: Abrir sesión automáticamente si viene de completar cita
    this.route.queryParams.subscribe(params => {
      if (params['newSession'] === 'true') {
        this.openForm();
      }
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

  loadMedicalRecords(patientId: number): void {
    this.medicalRecordService.getRecordsByPatientId(patientId).subscribe({
      next: (data) => this.medicalRecords = data.content,
      error: (err) => console.error('Error fetching medical records', err)
    });
  }

  openMedicalRecordForm(record?: MedicalRecord): void {
    this.selectedMedicalRecord = record;
    this.showMedicalRecordForm = true;
  }

  closeMedicalRecordForm(): void {
    this.showMedicalRecordForm = false;
    this.selectedMedicalRecord = undefined;
  }

  onMedicalRecordSaved(): void {
    this.closeMedicalRecordForm();
    if (this.patient?.id) {
      this.loadMedicalRecords(this.patient.id);
    }
  }

  async deleteMedicalRecord(id: number, event: Event): Promise<void> {
    event.stopPropagation();
    const confirmed = await this.notificationService.confirm(
      'Eliminar Registro',
      '¿Está seguro de que desea eliminar este registro médico?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.medicalRecordService.deleteRecord(id).subscribe({
        next: () => {
          this.toastService.show('Registro eliminado', 'success');
          if (this.patient?.id) {
            this.loadMedicalRecords(this.patient.id);
          }
        },
        error: () => this.toastService.show('Error al eliminar registro', 'error')
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

  togglePersonalDetails(): void {
    this.showPersonalDetails = !this.showPersonalDetails;
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
}
