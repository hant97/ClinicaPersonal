import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { PatientService } from '../../../core/services/patient/patient.service';
import { ClinicalSessionService } from '../../../core/services/clinical-session.service';
import { AppointmentService } from '../../../core/services/appointment.service';
import { RiskAlertService } from '../../../core/services/risk-alert.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ClinicalHistoryService } from '../../../core/services/clinical-history.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalSession } from '../../../core/models/clinical-session.model';
import { Appointment } from '../../../core/models/appointment.model';
import { RiskAlert } from '../../../core/models/risk-alert.model';
import { ClinicalHistory } from '../../../core/models/clinical-history.model';

export interface PatientAppointmentsSummary {
  upcoming: Appointment[];
  recent: Appointment[];
  next?: Appointment;
}

export interface PatientHistorySummary {
  history: ClinicalHistory;
  counts: Record<string, number>;
}

/**
 * Orquesta las llamadas HTTP y el cálculo de datos derivados que necesita
 * la vista de detalle de paciente (PatientDetailComponent).
 *
 * El componente conserva el estado de UI (pestañas activas, secciones
 * colapsadas, modales) y delega aquí todo lo relacionado con obtener y
 * combinar información del backend, para no mezclar ambas responsabilidades
 * en un solo archivo.
 */
@Injectable({
  providedIn: 'root'
})
export class PatientDetailDataService {
  constructor(
    private patientService: PatientService,
    private sessionService: ClinicalSessionService,
    private appointmentService: AppointmentService,
    private riskAlertService: RiskAlertService,
    private catalogService: CatalogService,
    private clinicalHistoryService: ClinicalHistoryService
  ) {}

  loadPatient(identifier: string | number): Observable<Patient> {
    return this.patientService.getById(identifier);
  }

  uploadPatientPhoto(patientId: number, file: File): Observable<Patient> {
    return this.patientService.uploadPhoto(patientId, file);
  }

  loadSessions(patientId: number): Observable<ClinicalSession[]> {
    return this.sessionService.getSessionsByPatientId(patientId).pipe(
      map(page => page.content)
    );
  }

  deleteSession(sessionId: number): Observable<void> {
    return this.sessionService.deleteSession(sessionId);
  }

  loadAppointmentsSummary(patientId: number): Observable<PatientAppointmentsSummary> {
    return this.appointmentService.getByPatientId(patientId, 0, 10).pipe(
      map(page => {
        const today = new Date().toISOString().split('T')[0];
        const all = page.content;
        const upcoming = all
          .filter(a => a.appointmentDate >= today && a.status !== 'CANCELADA' && a.status !== 'NO_ASISTIO' && a.status !== 'COMPLETADA')
          .sort((a, b) => a.appointmentDate.localeCompare(b.appointmentDate));
        const recent = all
          .filter(a => a.appointmentDate < today || a.status === 'COMPLETADA')
          .sort((a, b) => b.appointmentDate.localeCompare(a.appointmentDate))
          .slice(0, 3);
        return { upcoming, recent, next: upcoming.length > 0 ? upcoming[0] : undefined };
      })
    );
  }

  loadHistorySummary(patientId: number): Observable<PatientHistorySummary> {
    return this.clinicalHistoryService.get(patientId).pipe(
      map(history => ({
        history,
        counts: {
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
        }
      }))
    );
  }

  /**
   * Obtiene las alertas activas del paciente y resuelve sus etiquetas
   * (tipo y nivel) contra los catálogos correspondientes. Las alertas cuyo
   * tipo ya no existe en el catálogo activo se descartan, igual que antes.
   */
  loadActiveAlertsWithLabels(patientId: number, isPsychology: boolean): Observable<RiskAlert[]> {
    const catalogCode = isPsychology ? 'RISK_ALERT_TYPE' : 'RISK_ALERT_TYPE_DERM';

    return this.riskAlertService.getAlertsByPatientId(patientId, true).pipe(
      switchMap(alertsPage => {
        const alerts = alertsPage.content;
        if (alerts.length === 0) {
          return of([] as RiskAlert[]);
        }

        return forkJoin({
          types: this.catalogService.getActiveItemsByCatalogCode(catalogCode),
          levels: this.catalogService.getActiveItemsByCatalogCode('RISK_ALERT_LEVEL')
        }).pipe(
          map(({ types, levels }) => {
            const resolved: RiskAlert[] = [];
            alerts.forEach(alert => {
              const typeItem = types.find(t => t.itemCode === alert.type || t.itemName === alert.type);
              if (typeItem) {
                const levelItem = levels.find(l => l.itemCode === alert.level || l.itemName === alert.level);
                resolved.push({
                  ...alert,
                  type: typeItem.itemName,
                  level: levelItem ? levelItem.itemName : alert.level
                });
              }
            });
            return resolved;
          })
        );
      })
    );
  }

  resolveAlert(patientId: number, alertId: number): Observable<RiskAlert> {
    return this.riskAlertService.resolveAlert(patientId, alertId);
  }
}
