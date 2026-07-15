import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { PatientFormComponent } from '../patient-form/patient-form.component';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../shared/services/toast/toast.service';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [CommonModule, PatientFormComponent, RouterLink],
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.css']
})
export class PatientListComponent implements OnInit {
  patients: Patient[] = [];
  showModal = false;
  selectedPatientId: number | null = null;

  constructor(
    private patientService: PatientService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.patientService.getAll().subscribe(data => {
      this.patients = data;
    });
  }

  openModal(id?: number): void {
    this.selectedPatientId = id || null;
    this.showModal = true;
  }

  closeModal(refresh: boolean): void {
    this.showModal = false;
    this.selectedPatientId = null;
    if (refresh) {
      this.loadPatients();
    }
  }

  deletePatient(id: number): void {
    if (confirm('¿Estás seguro de que deseas eliminar este paciente?')) {
      this.patientService.delete(id).subscribe({
        next: () => {
          this.toastService.show('Paciente eliminado exitosamente', 'success');
          this.loadPatients();
        },
        error: () => {
          this.toastService.show('Error al eliminar el paciente', 'error');
        }
      });
    }
  }
}
