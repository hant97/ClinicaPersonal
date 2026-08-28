import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService, Supply, SupplyStats } from '../../../core/services/inventory.service';
import { InventoryTransaction } from '../../../core/models/inventory-transaction.model';
import { SpecialtyService } from '../../../core/services/specialty.service';
import { SpecialtyItem } from '../../../core/models/specialty.model';
import { InventoryFormComponent } from '../inventory-form/inventory-form.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { ViewPreferenceService } from '../../../shared/services/view-preference/view-preference.service';
import { ExportService } from '../../../shared/services/export/export.service';
import { fetchAllPages } from '../../../core/utils/pagination.util';
import { LucideAngularModule } from 'lucide-angular';
import {
  Search, Edit, Trash2, Plus, AlertTriangle, Package, Eye, X,
  History, LayoutGrid, List, CalendarClock, Wallet, TrendingUp, SlidersHorizontal, Download
} from '../../../shared/icons/lucide-icons';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-inventory-list',
  standalone: true,
  imports: [CommonModule, FormsModule, InventoryFormComponent, LucideAngularModule, PaginationComponent],
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
  readonly History = History;
  readonly LayoutGrid = LayoutGrid;
  readonly List = List;
  readonly CalendarClock = CalendarClock;
  readonly Wallet = Wallet;
  readonly TrendingUp = TrendingUp;
  readonly SlidersHorizontal = SlidersHorizontal;
  readonly Download = Download;

  filteredSupplies: Supply[] = [];
  specialties: SpecialtyItem[] = [];
  stats: SupplyStats | null = null;
  recentTransactions: InventoryTransaction[] = [];

  searchTerm: string = '';
  showModal = false;
  selectedSupplyId: number | null = null;
  viewImageUrl: string | null = null;
  viewImageName: string | null = null;
  viewMode: 'table' | 'cards' = 'table';

  // Adjustment modal
  adjustingSupply: Supply | null = null;
  adjustmentType: 'IN' | 'OUT' = 'IN';
  adjustmentQuantity: number = 1;
  adjustmentReason: string = 'RESTOCK';
  adjustmentNotes: string = '';
  isAdjusting = false;

  // Movements modal
  movementsSupply: Supply | null = null;
  movements: InventoryTransaction[] = [];
  isLoadingMovements = false;

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
    private specialtyService: SpecialtyService,
    private toastService: ToastService,
    private notificationService: NotificationService,
    private viewPreferenceService: ViewPreferenceService,
    private exportService: ExportService
  ) {}

  ngOnInit(): void {
    this.viewMode = this.viewPreferenceService.getViewMode<'table' | 'cards'>('inventory_view_mode', 'table', 'cards');
    
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
      this.loadSupplies();
    });

    this.loadSupplies();
    this.loadStats();
    this.loadRecentTransactions();
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

  loadStats(): void {
    this.inventoryService.getStats().subscribe({
      next: (stats) => this.stats = stats,
      error: (err) => console.error('Error fetching inventory stats', err)
    });
  }

  loadRecentTransactions(): void {
    this.inventoryService.getRecentTransactions(8).subscribe({
      next: (transactions) => this.recentTransactions = transactions,
      error: (err) => console.error('Error fetching recent transactions', err)
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

  exportSupplies(): void {
    const requestPage = (page: number, size: number) => this.inventoryService.getAllSupplies(this.searchTerm, page, size);

    fetchAllPages(requestPage).subscribe({
      next: (supplies) => {
        const dataToExport = supplies.map(supply => ({
          'Nombre': supply.name,
          'Descripción': supply.description || '',
          'Especialidad': this.getSpecialtyLabel(supply.specialty),
          'Unidad de Medida': supply.unit,
          'Stock Actual': supply.currentStock,
          'Stock Mínimo': supply.minStockLevel,
          'Nivel de Stock': this.getStockLevelLabel(supply),
          'Precio Unitario': supply.price ?? '',
          'Fecha de Vencimiento': supply.expirationDate || ''
        }));

        this.exportService.exportToCsv(dataToExport, 'Inventario_Insumos');
        this.toastService.show(`${supplies.length} insumos exportados`, 'success');
      },
      error: () => this.toastService.show('Error al exportar el inventario', 'error')
    });
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
      this.loadStats();
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

  // --- Adjustment ---
  openAdjustment(supply: Supply): void {
    this.adjustingSupply = supply;
    this.adjustmentType = 'IN';
    this.adjustmentQuantity = 1;
    this.adjustmentReason = 'RESTOCK';
    this.adjustmentNotes = '';
  }

  closeAdjustment(): void {
    this.adjustingSupply = null;
    this.isAdjusting = false;
  }

  onAdjustmentTypeChange(): void {
    this.adjustmentReason = this.adjustmentType === 'IN' ? 'RESTOCK' : 'CLINICAL_USAGE';
  }

  submitAdjustment(): void {
    const supply = this.adjustingSupply;
    if (!supply || !supply.id) return;
    if (!this.adjustmentQuantity || this.adjustmentQuantity <= 0) {
      this.toastService.show('Ingrese una cantidad válida', 'error');
      return;
    }

    const transaction: InventoryTransaction = {
      supplyId: supply.id,
      quantity: this.adjustmentQuantity,
      type: this.adjustmentType,
      reason: this.adjustmentReason as InventoryTransaction['reason'],
      notes: this.adjustmentNotes?.trim() || undefined
    };

    this.isAdjusting = true;
    this.inventoryService.recordTransaction(transaction).subscribe({
      next: () => {
        this.isAdjusting = false;
        this.toastService.show('Stock ajustado correctamente', 'success');
        this.closeAdjustment();
        this.loadSupplies();
        this.loadStats();
        this.loadRecentTransactions();
      },
      error: (err) => {
        this.isAdjusting = false;
        this.toastService.show(err.error?.message || 'Error al ajustar el stock', 'error');
      }
    });
  }

  // --- Movements ---
  openMovements(supply: Supply): void {
    this.movementsSupply = supply;
    this.movements = [];
    this.isLoadingMovements = true;
    if (supply.id) {
      this.inventoryService.getTransactionsBySupply(supply.id).subscribe({
        next: (transactions) => {
          this.movements = transactions;
          this.isLoadingMovements = false;
        },
        error: () => {
          this.isLoadingMovements = false;
          this.toastService.show('Error al cargar los movimientos', 'error');
        }
      });
    }
  }

  setViewMode(mode: 'table' | 'cards'): void {
    this.viewMode = mode;
    this.viewPreferenceService.setViewMode('inventory_view_mode', mode);
  }

  closeMovements(): void {
    this.movementsSupply = null;
    this.movements = [];
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
          this.loadStats();
        },
        error: () => {
          this.toastService.show('Error al eliminar el suministro', 'error');
        }
      });
    }
  }

  // --- Helpers ---
  getStockLevel(supply: Supply): 'critical' | 'low' | 'optimal' {
    if (supply.currentStock <= 0) return 'critical';
    if (supply.currentStock <= supply.minStockLevel) return 'low';
    return 'optimal';
  }

  getStockLevelLabel(supply: Supply): string {
    const level = this.getStockLevel(supply);
    if (level === 'critical') return 'Crítico';
    if (level === 'low') return 'Stock bajo';
    return 'Óptimo';
  }

  isExpiringSoon(supply: Supply): boolean {
    if (!supply.expirationDate) return false;
    const exp = new Date(supply.expirationDate + 'T00:00:00');
    const now = new Date();
    const diffDays = (exp.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);
    return diffDays >= 0 && diffDays <= 30;
  }

  getSpecialtyLabel(specialty?: string): string {
    if (!specialty || specialty === 'GENERAL') return 'General';
    const found = this.specialties.find(s => s.code.toUpperCase() === specialty.toUpperCase());
    if (found) return found.name;
    if (specialty === 'PSICOLOGIA') return 'Psicología';
    if (specialty === 'DERMATOLOGIA') return 'Dermatología';
    return specialty.charAt(0) + specialty.slice(1).toLowerCase();
  }

  typeLabel(type: string): string {
    if (type === 'IN') return 'Entrada';
    if (type === 'OUT') return 'Salida';
    return 'Ajuste';
  }

  reasonLabel(reason: string): string {
    const map: Record<string, string> = {
      BILLING: 'Facturación',
      CLINICAL_USAGE: 'Uso clínico',
      EXPIRED: 'Caducado',
      DAMAGED: 'Dañado',
      RESTOCK: 'Reposición'
    };
    return map[reason] || reason;
  }
}
