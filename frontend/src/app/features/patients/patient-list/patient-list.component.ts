import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient, PatientStats } from '../../../core/models/patient.model';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { Router, RouterLink } from '@angular/router';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { CatalogService } from '../../../core/services/catalog.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { fetchAllPages } from '../../../core/utils/pagination.util';
import { LucideAngularModule } from 'lucide-angular';
import {
  Search,
  Eye,
  Edit,
  Trash2,
  Plus,
  Download,
  AlertTriangle,
  Calendar,
  Users,
  LayoutGrid,
  List,
  MoreHorizontal,
  FileText,
  UserCheck,
  UserX,
  CalendarPlus,
  UserPlus,
  UserRound,
  X,
  Phone,
  Mail,
  CheckCircle2,
  XCircle
} from '../../../shared/icons/lucide-icons';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-patient-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    PaginationComponent,
    RouterLink,
    LucideAngularModule
  ],
  templateUrl: './patient-list.component.html',
})
export class PatientListComponent implements OnInit, OnDestroy {
  readonly Plus = Plus;
  readonly Search = Search;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly FileText = FileText;
  readonly Users = Users;
  readonly UserCheck = UserCheck;
  readonly UserX = UserX;
  readonly CalendarPlus = CalendarPlus;
  readonly AlertTriangle = AlertTriangle;
  readonly Eye = Eye;
  readonly LayoutGrid = LayoutGrid;
  readonly List = List;
  readonly MoreHorizontal = MoreHorizontal;
  readonly Download = Download;
  readonly UserPlus = UserPlus;
  readonly UserRound = UserRound;
  readonly X = X;
  readonly Phone = Phone;
  readonly Mail = Mail;
  readonly Calendar = Calendar;
  readonly CheckCircle2 = CheckCircle2;
  readonly XCircle = XCircle;

  patients: Patient[] = [];
  stats: PatientStats | null = null;
  genderMap = new Map<string, string>();
  genderOptions: { code: string; label: string }[] = [];

  searchTerm: string = '';
  filterGender: string = '';
  filterStatus: string = 'ALL';
  viewMode: 'table' | 'cards' = 'cards';

  showModal = false;
  selectedPatientId: number | null = null;
  openMenuPatientId: number | null = null;

  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading = false;
  loadError = false;

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private patientService: PatientService,
    private toastService: ToastService,
    private notificationService: NotificationService,
    private exportService: ExportService,
    private catalogService: CatalogService,
    private viewPreferenceService: ViewPreferenceService,
    private router: Router
  ) {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadPatients();
    });
  }

  ngOnInit(): void {
    this.viewMode = this.viewPreferenceService.getViewMode<'table' | 'cards'>('patients_view_mode', 'cards', 'cards');
    this.loadGenders();
    this.loadPatients();
    this.loadStats();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadGenders(): void {
    this.catalogService.getActiveItemsByCatalogCode('GENDER').subscribe({
      next: (items) => {
        this.genderOptions = items.map(item => ({ code: item.itemCode, label: item.itemName }));
        items.forEach(item => this.genderMap.set(item.itemCode, item.itemName));
      },
      error: () => console.error('Error loading genders')
    });
  }

  private get activeFilter(): boolean | null {
    return this.filterStatus === 'ALL' ? null : this.filterStatus === 'ACTIVE';
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterGender || this.filterStatus !== 'ALL');
  }

  loadPatients(): void {
    this.isLoading = true;
    this.loadError = false;
    const request = this.searchTerm
      ? this.patientService.search(this.searchTerm, this.currentPage, this.pageSize, this.activeFilter, this.filterGender || undefined)
      : this.patientService.getAll(this.currentPage, this.pageSize, this.activeFilter, this.filterGender || undefined);
    request.subscribe({
      next: (page) => {
        this.patients = page.content;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.loadError = true;
        this.toastService.show('Error al cargar los pacientes', 'error');
      }
    });
  }

  loadStats(): void {
    this.patientService.getStats().subscribe({
      next: (stats) => this.stats = stats,
      error: (err) => console.error('Error fetching patient stats', err)
    });
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value.trim().toLowerCase());
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadPatients();
  }

  clearFilters(): void {
    this.filterGender = '';
    this.filterStatus = 'ALL';
    this.currentPage = 0;
    this.loadPatients();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPatients();
  }

  getInitials(firstName?: string, lastName?: string): string {
    const f = (firstName || '').charAt(0).toUpperCase();
    const l = (lastName || '').charAt(0).toUpperCase();
    return `${f}${l}` || 'P';
  }

  getAge(patient: Patient): number | null {
    if (!patient.dateOfBirth) return null;
    const birth = new Date(patient.dateOfBirth);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) {
      age--;
    }
    return age;
  }

  isMinor(patient: Patient): boolean {
    const age = this.getAge(patient);
    return age !== null && age < 18;
  }

  getGenderLabel(gender?: string): string {
    return (gender && this.genderMap.get(gender)) || gender || '—';
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.openMenuPatientId = null;
  }

  toggleMenu(patientId?: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (!patientId) return;
    this.openMenuPatientId = this.openMenuPatientId === patientId ? null : patientId;
  }

  closeMenu(): void {
    this.openMenuPatientId = null;
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
      this.loadStats();
    }
  }

  scheduleAppointment(patientId?: number, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (patientId) {
      this.router.navigate(['/agenda'], {
        queryParams: { newAppointment: 'true', patientId: patientId }
      });
    }
  }

  async deletePatient(id: number, event?: Event): Promise<void> {
    if (event) {
      event.stopPropagation();
    }
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
          this.loadStats();
        },
        error: () => {
          this.toastService.show('Error al eliminar el paciente', 'error');
        }
      });
    }
  }

  async toggleActive(patient: Patient, event?: Event): Promise<void> {
    if (event) {
      event.stopPropagation();
    }
    if (!patient.id) return;
    const newActive = patient.active === false;
    const action = newActive ? 'reactivar' : 'dar de baja';
    const confirmed = await this.notificationService.confirm(
      newActive ? 'Reactivar Paciente' : 'Dar de Baja',
      `¿Está seguro de que desea ${action} a este paciente?`,
      'Sí, confirmar',
      'Cancelar'
    );
    if (confirmed) {
      this.patientService.update(patient.id, { ...patient, active: newActive }).subscribe({
        next: () => {
          this.toastService.show(newActive ? 'Paciente reactivado' : 'Paciente dado de baja', 'success');
          this.loadPatients();
          this.loadStats();
        },
        error: () => {
          this.toastService.show('Error al cambiar el estado del paciente', 'error');
        }
      });
    }
  }

  exportPatients(): void {
    const requestPage = (page: number, size: number) => this.searchTerm
      ? this.patientService.search(this.searchTerm, page, size, this.activeFilter, this.filterGender || undefined)
      : this.patientService.getAll(page, size, this.activeFilter, this.filterGender || undefined);

    fetchAllPages(requestPage).subscribe({
      next: (patients) => {
        const dataToExport = patients.map(patient => ({
          'Nombre Completo': `${patient.firstName} ${patient.lastName}`,
          'Documento': patient.identificationDocument || '',
          'Contacto': patient.contactNumber || '',
          'Email': patient.email || '',
          'Fecha Nac.': (patient.dateOfBirth || '').replace('T', ' '),
          'Género': this.getGenderLabel(patient.gender),
          'Dirección': patient.address || '',
          'Estado': patient.active === false ? 'Inactivo' : 'Activo'
        }));

        this.exportService.exportToCsv(dataToExport, 'Directorio_Pacientes');
        this.toastService.show(`${patients.length} pacientes exportados`, 'success');
      },
      error: () => this.toastService.show('Error al exportar los pacientes', 'error')
    });
  }

  setViewMode(mode: 'table' | 'cards'): void {
    this.viewMode = mode;
    this.viewPreferenceService.setViewMode('patients_view_mode', mode);
  }
}
