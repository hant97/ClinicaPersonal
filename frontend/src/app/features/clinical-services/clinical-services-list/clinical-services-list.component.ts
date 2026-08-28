import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { ClinicalServiceService, ClinicalServiceFilters } from '../../../core/services/clinical-service.service';
import { ClinicalService, ClinicalServiceStats } from '../../../core/models/clinical-service.model';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule } from 'lucide-angular';
import {
  Plus, Edit, Trash2, Search, Activity, Clock, Eye,
  LayoutGrid, List, TrendingUp, Package, Wallet, X
} from '../../../shared/icons/lucide-icons';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ClinicalServicesFormComponent } from '../clinical-services-form/clinical-services-form.component';

import { SpecialtyService } from '../../../core/services/specialty.service';
import { SpecialtyItem } from '../../../core/models/specialty.model';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';

const SERVICE_CATEGORIES = ['Evaluación', 'Terapia', 'Procedimiento', 'Control', 'Diagnóstico', 'Otro'];

@Component({
  selector: 'app-clinical-services-list',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, PaginationComponent, ClinicalServicesFormComponent, DecimalPipe],
  templateUrl: './clinical-services-list.component.html'
})
export class ClinicalServicesListComponent implements OnInit, OnDestroy {
  services: ClinicalService[] = [];
  stats: ClinicalServiceStats | null = null;
  specialties: SpecialtyItem[] = [];
  categories = SERVICE_CATEGORIES;

  // Icons
  readonly Plus = Plus;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Search = Search;
  readonly Activity = Activity;
  readonly Clock = Clock;
  readonly Eye = Eye;
  readonly LayoutGrid = LayoutGrid;
  readonly List = List;
  readonly TrendingUp = TrendingUp;
  readonly Package = Package;
  readonly Wallet = Wallet;
  readonly X = X;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  isLoading = false;
  loadError = false;

  // Filters
  searchTerm = '';
  filterCategory = '';
  filterStatus = 'ALL';
  filterMinPrice: number | null = null;
  filterMaxPrice: number | null = null;
  viewMode: 'cards' | 'table' = 'cards';

  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Modal state
  showModal = false;
  selectedServiceId: number | null = null;
  viewingService: ClinicalService | null = null;

  constructor(
    private serviceService: ClinicalServiceService,
    private specialtyService: SpecialtyService,
    private toastService: ToastService,
    private notificationService: NotificationService,
    private viewPreferenceService: ViewPreferenceService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewPreferenceService.getViewMode<'cards' | 'table'>('clinical_services_view_mode', 'cards', 'cards');

    this.specialtyService.getActiveSpecialties().subscribe({
      next: (list) => this.specialties = list || [],
      error: () => {}
    });

    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0;
      this.loadServices();
    });

    this.loadServices();
    this.loadStats();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private currentFilters(): ClinicalServiceFilters {
    return {
      name: this.searchTerm || undefined,
      category: this.filterCategory || undefined,
      active: this.filterStatus === 'ALL' ? null : this.filterStatus === 'ACTIVE',
      minPrice: this.filterMinPrice ?? null,
      maxPrice: this.filterMaxPrice ?? null
    };
  }

  get hasActiveFilters(): boolean {
    return !!(this.filterCategory || this.filterStatus !== 'ALL' || this.filterMinPrice !== null || this.filterMaxPrice !== null);
  }

  loadServices(): void {
    this.isLoading = true;
    this.loadError = false;
    this.serviceService.getAllServices(this.currentPage, this.pageSize, this.currentFilters())
      .subscribe({
        next: (response) => {
          this.services = response.content;
          this.totalPages = response.page.totalPages;
          this.totalElements = response.page.totalElements;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
          this.loadError = true;
          this.toastService.show('Error al cargar la lista de servicios', 'error');
        }
      });
  }

  loadStats(): void {
    this.serviceService.getStats().subscribe({
      next: (stats) => this.stats = stats,
      error: (err) => console.error('Error fetching service stats', err)
    });
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadServices();
  }

  clearFilters(): void {
    this.filterCategory = '';
    this.filterStatus = 'ALL';
    this.filterMinPrice = null;
    this.filterMaxPrice = null;
    this.currentPage = 0;
    this.loadServices();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadServices();
  }

  openModal(serviceId: number | null = null): void {
    this.selectedServiceId = serviceId;
    this.showModal = true;
  }

  closeModal(refresh: boolean): void {
    this.showModal = false;
    this.selectedServiceId = null;
    if (refresh) {
      this.loadServices();
      this.loadStats();
    }
  }

  viewService(service: ClinicalService): void {
    this.viewingService = service;
  }

  closeView(): void {
    this.viewingService = null;
  }

  async deleteService(id: number): Promise<void> {
    const confirmed = await this.notificationService.confirm(
      'Eliminar servicio',
      '¿Está seguro de que desea eliminar este servicio?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.serviceService.deleteService(id).subscribe({
        next: () => {
          this.toastService.show('Servicio eliminado exitosamente', 'success');
          this.loadServices();
          this.loadStats();
        },
        error: () => {
          this.toastService.show('Error al eliminar el servicio', 'error');
        }
      });
    }
  }

  getTopBarWidth(total: number, list: { total: number }[]): number {
    const max = Math.max(...list.map(s => s.total));
    return max > 0 ? Math.round((total / max) * 100) : 0;
  }

  getSpecialtyLabel(specialty?: string): string {
    if (!specialty || specialty === 'GENERAL') return 'General';
    const found = this.specialties.find(s => s.code.toUpperCase() === specialty.toUpperCase());
    if (found) return found.name;
    if (specialty === 'PSICOLOGIA') return 'Psicología';
    if (specialty === 'DERMATOLOGIA') return 'Dermatología';
    return specialty.charAt(0) + specialty.slice(1).toLowerCase();
  }

  setViewMode(mode: 'cards' | 'table'): void {
    this.viewMode = mode;
    this.viewPreferenceService.setViewMode('clinical_services_view_mode', mode);
  }
}
