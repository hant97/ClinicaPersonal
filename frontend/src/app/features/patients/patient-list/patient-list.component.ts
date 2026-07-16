import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { PatientFormComponent } from '../patient-form/patient-form.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { LucideAngularModule, Search, Eye, Edit, Trash2, Plus, Download, AlertTriangle } from 'lucide-angular';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [CommonModule, PatientFormComponent, PaginationComponent, RouterLink, LucideAngularModule],
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
  searchTerm: string = '';
  showModal = false;
  selectedPatientId: number | null = null;
  
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;

  private searchSubject = new Subject<string>();

  constructor(
    private patientService: PatientService,
    private toastService: ToastService,
    private notificationService: NotificationService,
    private exportService: ExportService
  ) {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadPatients();
    });
  }

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    if (this.searchTerm) {
      this.patientService.search(this.searchTerm, this.currentPage, this.pageSize).subscribe(page => {
        this.patients = page.content;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
      });
    } else {
      this.patientService.getAll(this.currentPage, this.pageSize).subscribe(page => {
        this.patients = page.content;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
      });
    }
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value.trim().toLowerCase());
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPatients();
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
    const dataToExport = this.patients.map(patient => ({
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
