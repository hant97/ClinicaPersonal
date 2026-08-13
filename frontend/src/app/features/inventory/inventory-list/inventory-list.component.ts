import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InventoryService, Supply } from '../../../core/services/inventory.service';
import { InventoryFormComponent } from '../inventory-form/inventory-form.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Search, Edit, Trash2, Plus, AlertTriangle, Package, Eye, X } from 'lucide-angular';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-inventory-list',
  standalone: true,
  imports: [CommonModule, InventoryFormComponent, LucideAngularModule, PaginationComponent],
  templateUrl: './inventory-list.component.html'
})
export class InventoryListComponent implements OnInit, OnDestroy {
  readonly Search = Search;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Plus = Plus;
  readonly AlertTriangle = AlertTriangle;
  readonly Package = Package;
  readonly Eye = Eye;
  readonly X = X;

  filteredSupplies: Supply[] = [];
  searchTerm: string = '';
  showModal = false;
  selectedSupplyId: number | null = null;
  viewImageUrl: string | null = null;
  viewImageName: string | null = null;
  
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  isLoading = false;
  loadError = false;
  
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  constructor(
    private inventoryService: InventoryService,
    private toastService: ToastService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm = term;
      this.currentPage = 0; // Reset to first page on search
      this.loadSupplies();
    });

    this.loadSupplies();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadSupplies(): void {
    this.isLoading = true;
    this.loadError = false;
    this.inventoryService.getAllSupplies(this.searchTerm, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.filteredSupplies = page.content;
        this.totalPages = page.page.totalPages;
        this.totalElements = page.page.totalElements;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.loadError = true;
        this.toastService.show('Error al cargar el inventario', 'error');
      }
    });
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchSubject.next(target.value.toLowerCase());
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadSupplies();
  }

  openModal(id?: number): void {
    this.selectedSupplyId = id || null;
    this.showModal = true;
  }

  closeModal(refresh: boolean): void {
    this.showModal = false;
    this.selectedSupplyId = null;
    if (refresh) {
      this.loadSupplies();
    }
  }

  openImageView(supply: Supply): void {
    this.viewImageUrl = supply.imageUrl ?? null;
    this.viewImageName = supply.name;
  }

  closeImageView(): void {
    this.viewImageUrl = null;
    this.viewImageName = null;
  }

  async deleteSupply(id: number): Promise<void> {
    const confirmed = await this.notificationService.confirm(
      'Eliminar Suministro',
      '¿Estás seguro de que deseas eliminar este suministro?',
      'Sí, eliminar',
      'Cancelar'
    );
    if (confirmed) {
      this.inventoryService.deleteSupply(id).subscribe({
        next: () => {
          this.toastService.show('Suministro eliminado exitosamente', 'success');
          this.loadSupplies();
        },
        error: () => {
          this.toastService.show('Error al eliminar el suministro', 'error');
        }
      });
    }
  }
}
