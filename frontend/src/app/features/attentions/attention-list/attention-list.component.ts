import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { LucideAngularModule } from 'lucide-angular';
import {
  Stethoscope,
  CalendarCheck,
  Clock,
  User,
  Plus,
  Search,
  FilterX,
  Play,
  CheckCircle2,
  Receipt,
  FileText,
  Trash2,
  DollarSign,
  Check,
  ChevronRight,
  Sparkles,
  X,
  RefreshCw,
  AlertCircle,
  Pill,
  ArrowRight,
  MoreVertical,
  Activity
} from '../../../shared/icons/lucide-icons';
import { AttentionService } from '../../../core/services/attention.service';
import { Attention, AttentionStatus, AttentionSummary } from '../../../core/models/attention.model';
import { PatientService } from '../../../core/services/patient/patient.service';
import { Patient } from '../../../core/models/patient.model';
import { ClinicalServiceService } from '../../../core/services/clinical-service.service';
import { ClinicalService } from '../../../core/models/clinical-service.model';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { FocusTrapDirective } from '../../../shared/directives/focus-trap.directive';

@Component({
  selector: 'app-attention-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    LucideAngularModule,
    PaginationComponent,
    FocusTrapDirective
  ],
  templateUrl: './attention-list.component.html'
})
export class AttentionListComponent implements OnInit, OnDestroy {
  // Lucide Icons
  readonly Stethoscope = Stethoscope;
  readonly CalendarCheck = CalendarCheck;
  readonly Clock = Clock;
  readonly User = User;
  readonly Plus = Plus;
  readonly Search = Search;
  readonly FilterX = FilterX;
  readonly Play = Play;
  readonly CheckCircle2 = CheckCircle2;
  readonly Receipt = Receipt;
  readonly FileText = FileText;
  readonly Trash2 = Trash2;
  readonly DollarSign = DollarSign;
  readonly Check = Check;
  readonly ChevronRight = ChevronRight;
  readonly Sparkles = Sparkles;
  readonly X = X;
  readonly RefreshCw = RefreshCw;
  readonly AlertCircle = AlertCircle;
  readonly Pill = Pill;
  readonly ArrowRight = ArrowRight;
  readonly MoreVertical = MoreVertical;
  readonly Activity = Activity;

  attentions: Attention[] = [];
  summary: AttentionSummary | null = null;
  loading = false;
  loadingSummary = false;

  // Filters
  searchTerm = '';
  private searchSubject = new Subject<string>();
  private searchSubscription?: Subscription;

  statusFilter: string = 'ALL';
  dateFilter: 'TODAY' | 'YESTERDAY' | 'WEEK' | 'ALL' | 'CUSTOM' = 'TODAY';
  customStartDate = '';
  customEndDate = '';
  selectedProfessionalId: number | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 15;
  totalElements = 0;
  totalPages = 0;

  // Ancillary Data
  clinicalServices: ClinicalService[] = [];
  availablePatients: Patient[] = [];
  patientSearchQuery = '';
  filteredPatients: Patient[] = [];
  isSearchingPatients = false;

  // Quick Attention Modal
  showQuickModal = false;
  quickForm = {
    patientId: null as number | null,
    selectedPatient: null as Patient | null,
    clinicalServiceId: null as number | null,
    motive: '',
    notes: '',
    initialStatus: 'EN_PROCESO' as AttentionStatus
  };

  // Status Action Modal
  showStatusModal = false;
  targetAttention: Attention | null = null;
  nextStatus: AttentionStatus | null = null;
  statusModalNotes = '';

  constructor(
    private attentionService: AttentionService,
    private patientService: PatientService,
    private clinicalServiceService: ClinicalServiceService,
    private userService: UserService,
    private toastService: ToastService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.searchSubscription = this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged()
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadAttentions();
    });

    this.loadTodaySummary();
    this.loadAttentions();
    this.loadClinicalServices();
  }

  ngOnDestroy(): void {
    this.searchSubscription?.unsubscribe();
  }

  onSearchChange(): void {
    this.searchSubject.next(this.searchTerm);
  }

  loadTodaySummary(): void {
    this.loadingSummary = true;
    this.attentionService.getTodaySummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loadingSummary = false;
      },
      error: () => {
        this.loadingSummary = false;
      }
    });
  }

  loadAttentions(): void {
    this.loading = true;

    let startDate: string | undefined;
    let endDate: string | undefined;
    const now = new Date();

    if (this.dateFilter === 'TODAY') {
      const todayStr = this.formatDate(now);
      startDate = todayStr;
      endDate = todayStr;
    } else if (this.dateFilter === 'YESTERDAY') {
      const yesterday = new Date(now);
      yesterday.setDate(now.getDate() - 1);
      const yestStr = this.formatDate(yesterday);
      startDate = yestStr;
      endDate = yestStr;
    } else if (this.dateFilter === 'WEEK') {
      const weekStart = new Date(now);
      weekStart.setDate(now.getDate() - 6);
      startDate = this.formatDate(weekStart);
      endDate = this.formatDate(now);
    } else if (this.dateFilter === 'CUSTOM') {
      startDate = this.customStartDate || undefined;
      endDate = this.customEndDate || undefined;
    }

    this.attentionService.getAll(
      this.searchTerm,
      this.statusFilter,
      this.selectedProfessionalId || undefined,
      undefined,
      startDate,
      endDate,
      this.currentPage,
      this.pageSize
    ).subscribe({
      next: (res) => {
        this.attentions = res.content || [];
        this.totalElements = res.page?.totalElements || 0;
        this.totalPages = res.page?.totalPages || 0;
        this.loading = false;
      },
      error: (err) => {
        this.toastService.show('Error al cargar la lista de atenciones', 'error');
        this.loading = false;
      }
    });
  }

  loadClinicalServices(): void {
    this.clinicalServiceService.getAllActiveServices().subscribe({
      next: (services) => {
        this.clinicalServices = services;
      },
      error: () => {}
    });
  }

  setDateFilter(filter: 'TODAY' | 'YESTERDAY' | 'WEEK' | 'ALL' | 'CUSTOM'): void {
    this.dateFilter = filter;
    this.currentPage = 0;
    this.loadAttentions();
  }

  setStatusFilter(status: string): void {
    this.statusFilter = status;
    this.currentPage = 0;
    this.loadAttentions();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.statusFilter = 'ALL';
    this.dateFilter = 'TODAY';
    this.customStartDate = '';
    this.customEndDate = '';
    this.selectedProfessionalId = null;
    this.currentPage = 0;
    this.loadAttentions();
    this.loadTodaySummary();
  }

  onPageChange(newPage: number): void {
    this.currentPage = newPage;
    this.loadAttentions();
  }

  // Quick Action Lifecycle Workflow
  startAttention(attention: Attention): void {
    if (!attention.id) return;
    this.attentionService.updateStatus(attention.id, 'EN_PROCESO').subscribe({
      next: (updated) => {
        this.toastService.show('Atención iniciada en consulta', 'success');
        this.updateLocalAttention(updated);
        this.loadTodaySummary();
      },
      error: () => {
        this.toastService.show('No se pudo iniciar la atención', 'error');
      }
    });
  }

  openStatusModal(attention: Attention, status: AttentionStatus): void {
    this.targetAttention = attention;
    this.nextStatus = status;
    this.statusModalNotes = '';
    this.showStatusModal = true;
  }

  confirmStatusChange(): void {
    if (!this.targetAttention?.id || !this.nextStatus) return;

    this.attentionService.updateStatus(
      this.targetAttention.id,
      this.nextStatus,
      this.statusModalNotes.trim() ? this.statusModalNotes : undefined
    ).subscribe({
      next: (updated) => {
        this.toastService.show(`Estado actualizado a ${this.getStatusLabel(this.nextStatus!)}`, 'success');
        this.updateLocalAttention(updated);
        this.loadTodaySummary();
        this.showStatusModal = false;
        this.targetAttention = null;
        this.nextStatus = null;
      },
      error: () => {
        this.toastService.show('Error al actualizar estado de la atención', 'error');
      }
    });
  }

  // Navigation shortcuts
  goToClinicalSession(attention: Attention): void {
    if (attention.clinicalSessionId) {
      this.router.navigate([`/patients/${attention.patientId}/sessions/${attention.clinicalSessionId}/edit`]);
    } else {
      this.router.navigate([`/patients/${attention.patientId}/sessions/new`], {
        queryParams: {
          attentionId: attention.id,
          appointmentId: attention.appointmentId,
          date: attention.attentionDate,
          startTime: attention.startTime,
          endTime: attention.endTime
        }
      });
    }
  }

  goToPrescription(attention: Attention): void {
    this.router.navigate([`/patients/${attention.patientId}`], {
      queryParams: { tab: 'prescriptions', attentionId: attention.id }
    });
  }

  goToBilling(attention: Attention): void {
    if (attention.paymentId) {
      this.router.navigate(['/billing'], {
        queryParams: { paymentId: attention.paymentId }
      });
    } else {
      this.router.navigate(['/billing'], {
        queryParams: {
          newPayment: 'true',
          patientId: attention.patientId,
          attentionId: attention.id,
          clinicalServiceId: attention.clinicalServiceId,
          appointmentId: attention.appointmentId,
          description: attention.clinicalServiceName
            ? `Cobro de ${attention.clinicalServiceName}`
            : `Cobro de atención médica (${attention.attentionDate})`
        }
      });
    }
  }

  deleteAttention(attention: Attention): void {
    if (!attention.id) return;
    if (!confirm('¿Estás seguro de eliminar este registro de atención?')) return;

    this.attentionService.delete(attention.id).subscribe({
      next: () => {
        this.toastService.show('Atención eliminada correctamente', 'success');
        this.attentions = this.attentions.filter(a => a.id !== attention.id);
        this.loadTodaySummary();
      },
      error: () => {
        this.toastService.show('No se pudo eliminar la atención', 'error');
      }
    });
  }

  // Quick Capture Modal
  openQuickModal(): void {
    this.quickForm = {
      patientId: null,
      selectedPatient: null,
      clinicalServiceId: null,
      motive: '',
      notes: '',
      initialStatus: 'EN_PROCESO'
    };
    this.patientSearchQuery = '';
    this.filteredPatients = [];
    this.showQuickModal = true;
  }

  closeQuickModal(): void {
    this.showQuickModal = false;
  }

  searchPatients(): void {
    const query = this.patientSearchQuery.trim();
    if (query.length < 2) {
      this.filteredPatients = [];
      return;
    }

    this.isSearchingPatients = true;
    this.patientService.search(query, 0, 10).subscribe({
      next: (res) => {
        this.filteredPatients = res.content;
        this.isSearchingPatients = false;
      },
      error: () => {
        this.isSearchingPatients = false;
      }
    });
  }

  selectPatient(patient: Patient): void {
    this.quickForm.patientId = patient.id ?? null;
    this.quickForm.selectedPatient = patient;
    this.patientSearchQuery = `${patient.firstName} ${patient.lastName} (${patient.identificationDocument})`;
    this.filteredPatients = [];
  }

  submitQuickAttention(): void {
    if (!this.quickForm.patientId) {
      this.toastService.show('Selecciona un paciente para la atención', 'warning');
      return;
    }

    const payload: Partial<Attention> = {
      patientId: this.quickForm.patientId,
      clinicalServiceId: this.quickForm.clinicalServiceId || undefined,
      motive: this.quickForm.motive,
      notes: this.quickForm.notes,
      status: this.quickForm.initialStatus,
      attentionDate: this.formatDate(new Date())
    };

    this.attentionService.create(payload).subscribe({
      next: (created) => {
        this.toastService.show('Atención registrada con éxito', 'success');
        this.closeQuickModal();
        this.loadTodaySummary();
        this.loadAttentions();
      },
      error: () => {
        this.toastService.show('Error al registrar la atención', 'error');
      }
    });
  }

  // Helpers
  private updateLocalAttention(updated: Attention): void {
    const index = this.attentions.findIndex(a => a.id === updated.id);
    if (index !== -1) {
      this.attentions[index] = updated;
    } else {
      this.attentions.unshift(updated);
    }
  }

  private formatDate(d: Date): string {
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getStatusBadgeClass(status: AttentionStatus): string {
    switch (status) {
      case 'AGENDADA':
        return 'bg-blue-50 text-blue-700 border-blue-200';
      case 'EN_PROCESO':
        return 'bg-amber-50 text-amber-700 border-amber-300 ring-1 ring-amber-400/40 animate-pulse';
      case 'ATENDIDA':
        return 'bg-purple-50 text-purple-700 border-purple-200';
      case 'COBRADA':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'CANCELADA':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200';
    }
  }

  getStatusLabel(status: AttentionStatus): string {
    switch (status) {
      case 'AGENDADA': return 'Agendada';
      case 'EN_PROCESO': return 'En Proceso';
      case 'ATENDIDA': return 'Atendida';
      case 'COBRADA': return 'Cobrada';
      case 'CANCELADA': return 'Cancelada';
      default: return status;
    }
  }

  getProfessionalDisplayName(name?: string): string {
    if (!name || name.trim() === '' || name.trim() === 'null null') {
      return 'Profesional';
    }
    return name;
  }
}
