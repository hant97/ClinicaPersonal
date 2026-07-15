import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Appointment } from '../../../core/models/appointment.model';
import { AppointmentFormComponent } from '../appointment-form/appointment-form.component';
import { PatientService } from '../../../core/services/patient/patient.service';
import { LucideAngularModule, Plus, Calendar, MoreVertical } from 'lucide-angular';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [CommonModule, AppointmentFormComponent, LucideAngularModule],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.css'
})
export class AgendaComponent implements OnInit {
  readonly Plus = Plus;
  readonly Calendar = Calendar;
  readonly MoreVertical = MoreVertical;

  appointments: Appointment[] = [];
  showForm = false;
  openMenuId: number | null = null;
  appointmentToEdit: Appointment | null = null;
  patientMap = new Map<number, string>();

  constructor(
    private appointmentService: AppointmentService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    // Para no hacer una llamada por cada cita, precargamos los pacientes
    this.patientService.getAll().subscribe({
      next: (patients) => {
        patients.forEach(p => {
          if (p.id) {
            this.patientMap.set(Number(p.id), `${p.firstName} ${p.lastName}`);
          }
        });
        this.loadAppointments();
      },
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  loadAppointments(): void {
    this.appointmentService.getAll().subscribe({
      next: (data) => {
        this.appointments = data
          .filter(app => app.status !== 'CANCELLED')
          .map(app => ({
            ...app,
            patientName: this.patientMap.get(Number(app.patientId)) || 'Paciente Desconocido'
          }));
      },
      error: (err) => console.error('Error fetching appointments', err)
    });
  }

  openForm(): void {
    this.appointmentToEdit = null;
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.appointmentToEdit = null;
  }

  onAppointmentSaved(): void {
    this.showForm = false;
    this.appointmentToEdit = null;
    this.loadAppointments();
  }

  toggleMenu(id: number | undefined): void {
    if (!id) return;
    if (this.openMenuId === id) {
      this.openMenuId = null;
    } else {
      this.openMenuId = id;
    }
  }

  confirmAppointment(appointment: Appointment): void {
    this.openMenuId = null;
    if (appointment.id) {
      this.appointmentService.updateStatus(appointment.id, 'CONFIRMED').subscribe({
        next: () => this.loadAppointments(),
        error: (err) => console.error('Error confirming appointment', err)
      });
    }
  }

  cancelAppointment(appointment: Appointment): void {
    this.openMenuId = null;
    if (confirm('¿Estás seguro de cancelar esta cita? Esta acción no se puede deshacer.')) {
      if (appointment.id) {
        this.appointmentService.updateStatus(appointment.id, 'CANCELLED').subscribe({
          next: () => this.loadAppointments(),
          error: (err) => console.error('Error cancelling appointment', err)
        });
      }
    }
  }

  openRescheduleForm(appointment: Appointment): void {
    this.openMenuId = null;
    this.appointmentToEdit = appointment;
    this.showForm = true;
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'status-scheduled';
      case 'CONFIRMED': return 'status-confirmed';
      case 'ATTENDED': return 'status-attended';
      case 'MISSED': return 'status-missed';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Programada';
      case 'CONFIRMED': return 'Confirmada';
      case 'ATTENDED': return 'Atendida';
      case 'MISSED': return 'No asistió';
      case 'CANCELLED': return 'Cancelada';
      default: return status;
    }
  }
}
