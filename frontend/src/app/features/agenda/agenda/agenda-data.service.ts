import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AttentionService } from '../../../core/services/attention.service';
import { ScheduleBlockService } from '../../../core/services/schedule-block.service';
import { UserService } from '../../../core/services/user.service';
import { Appointment } from '../../../core/models/appointment.model';
import { ScheduleBlock } from '../../../core/models/schedule-block.model';
import { UserProfile } from '../../../core/models/user-profile.model';
import { Attention } from '../../../core/models/attention.model';

/**
 * Orquesta las llamadas HTTP que necesita la vista de agenda
 * (AgendaComponent): citas, bloqueos de horario, profesionales y el
 * inicio de una atención desde una cita.
 *
 * El componente conserva el estado de UI (vista calendario/lista, filtros,
 * modales, menús) y delega aquí todo lo relacionado con obtener y actualizar
 * información del backend.
 */
@Injectable({
  providedIn: 'root'
})
export class AgendaDataService {
  constructor(
    private appointmentService: AppointmentService,
    private attentionService: AttentionService,
    private blockService: ScheduleBlockService,
    private userService: UserService
  ) {}

  loadProfessionals(): Observable<UserProfile[]> {
    return this.userService.getProfessionals();
  }

  countAppointmentsOnDate(dateKey: string): Observable<number> {
    return this.appointmentService.search(undefined, 'ALL', dateKey, dateKey).pipe(
      map(page => page.content.length)
    );
  }

  searchAppointments(
    searchTerm: string,
    status: string,
    startDate?: string,
    endDate?: string,
    professionalId?: number
  ): Observable<Appointment[]> {
    return this.appointmentService.search(searchTerm, status, startDate, endDate, professionalId).pipe(
      map(page => page.content)
    );
  }

  loadBlocks(startDate?: string, endDate?: string, professionalId?: number): Observable<ScheduleBlock[]> {
    return this.blockService.getBlocks(startDate, endDate, professionalId);
  }

  updateAppointmentStatus(appointmentId: number, status: string, updateSeries: boolean): Observable<Appointment> {
    return this.appointmentService.updateStatus(appointmentId, status, updateSeries);
  }

  startAttentionFromAppointment(appointmentId: number): Observable<Attention> {
    return this.attentionService.createFromAppointment(appointmentId);
  }
}
