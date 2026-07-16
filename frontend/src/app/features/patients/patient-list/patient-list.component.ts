import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { PatientFormComponent } from '../patient-form/patient-form.component';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { LucideAngularModule, Search, Eye, Edit, Trash2, Plus, Download, AlertTriangle } from 'lucide-angular';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [CommonModule, PatientFormComponent, RouterLink, LucideAngularModule],
  templateUrl: './patient-list.component.html',
  styleUrls: ['./patient-list.component.css']
})
export class PatientListComponent implements OnInit {
  readonly Search = Search;
  readonly Eye = Eye;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Plus = Plus;
  readonly Download = Download;
  readonly AlertTriangle = AlertTriangle;

  patients: Patient[] = [];
  filteredPatients: Patient[] = [];
  searchTerm: string = '';
  showModal = false;
  selectedPatientId: number | null = null;

  constructor(
    private patientService: PatientService,
    private toastService: ToastService,
    private notificationService: NotificationService,
    private exportService: ExportService
  ) {}

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.patientService.getAll().subscribe(data => {
      this.patients = data;
      this.filterPatients();
    });
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm = target.value.toLowerCase();
    this.filterPatients();
  }

  filterPatients(): void {
    if (!this.searchTerm) {
      this.filteredPatients = [...this.patients];
    } else {
      this.filteredPatients = this.patients.filter(patient => {
        const fullName = `${patient.firstName} ${patient.lastName}`.toLowerCase();
        const doc = (patient.identificationDocument || '').toLowerCase();
        return fullName.includes(this.searchTerm) || doc.includes(this.searchTerm);
      });
    }
  }

  getInitials(firstName: string, lastName: string): string {
    return (firstName.charAt(0) + lastName.charAt(0)).toUpperCase();
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

  async deletePatient(id: number): Promise<void> {
    const confirmed = await this.notificationService.confirm(
      'Eliminar Paciente',
      '¿Estás seguro de que deseas eliminar este paciente? Esta acción no se puede deshacer.',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
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

  exportPatients(): void {
    const dataToExport = this.filteredPatients.map(patient => ({
      'Nombre Completo': `${patient.firstName} ${patient.lastName}`,
      'Documento': patient.identificationDocument || '',
      'Contacto': patient.contactNumber || '',
      'Email': patient.email || '',
      'Fecha Nac.': (patient.dateOfBirth || '').replace('T', ' '),
      'Género': patient.gender || '',
      'Dirección': patient.address || ''
    }));

    this.exportService.exportToExcel(dataToExport, 'Directorio_Pacientes');
  }
}
