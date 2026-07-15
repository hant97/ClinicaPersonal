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

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [CommonModule, ClinicalSessionFormComponent, AssessmentListComponent],
  templateUrl: './patient-detail.component.html',
  styleUrls: ['./patient-detail.component.css']
})
export class PatientDetailComponent implements OnInit {
  patient: Patient | null = null;
  sessions: ClinicalSession[] = [];
  showForm = false;
  expandedSessionId: number | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private notificationService: NotificationService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPatient(Number(id));
      this.loadSessions(Number(id));
    }
    
    // Fase 2: Abrir sesión automáticamente si viene de completar cita
    this.route.queryParams.subscribe(params => {
      if (params['newSession'] === 'true') {
        this.openForm();
      }
    });
  }

  loadPatient(id: number): void {
    this.patientService.getById(id).subscribe({
      next: (data) => this.patient = data,
      error: (err) => console.error('Error fetching patient', err)
    });
  }

  loadSessions(patientId: number): void {
    this.sessionService.getSessionsByPatientId(patientId).subscribe({
      next: (data) => this.sessions = data,
      error: (err) => console.error('Error fetching sessions', err)
    });
  }

  openForm(): void {
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onSessionSaved(): void {
    this.showForm = false;
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
    // TODO: Implement edit logic
    this.toastService.show('Funcionalidad de edición pendiente de implementar.', 'info');
  }
}
