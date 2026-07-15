import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppointmentService } from '../../../core/services/appointment.service';
import { Appointment } from '../../../core/models/appointment.model';
import { AppointmentFormComponent } from '../appointment-form/appointment-form.component';
import { PatientService } from '../../../core/services/patient/patient.service';

@Component({
  selector: 'app-agenda',
  standalone: true,
  imports: [CommonModule, AppointmentFormComponent],
  templateUrl: './agenda.component.html',
  styleUrl: './agenda.component.css'
})
export class AgendaComponent implements OnInit {
  appointments: Appointment[] = [];
  showForm = false;
  patientMap = new Map<number, string>();

  constructor(
    private appointmentService: AppointmentService,
    private patientService: PatientService
  ) {}

  ngOnInit(): void {
    // Para no hacer una llamada por cada cita, precargamos los pacientes
    this.patientService.getAll().subscribe({
      next: (patients) => {
        patients.forEach(p => this.patientMap.set(p.id!, `${p.firstName} ${p.lastName}`));
        this.loadAppointments();
      },
      error: (err) => console.error('Error fetching patients', err)
    });
  }

  loadAppointments(): void {
    this.appointmentService.getAll().subscribe({
      next: (data) => {
        this.appointments = data.map(app => ({
          ...app,
          patientName: this.patientMap.get(app.patientId) || 'Paciente Desconocido'
        }));
      },
      error: (err) => console.error('Error fetching appointments', err)
    });
  }

  openForm(): void {
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
  }

  onAppointmentSaved(): void {
    this.showForm = false;
    this.loadAppointments();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'status-scheduled';
      case 'CONFIRMED': return 'status-confirmed';
      case 'ATTENDED': return 'status-attended';
      case 'MISSED': return 'status-missed';
      default: return '';
    }
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'SCHEDULED': return 'Programada';
      case 'CONFIRMED': return 'Confirmada';
      case 'ATTENDED': return 'Atendida';
      case 'MISSED': return 'No asistió';
      default: return status;
    }
  }
}
