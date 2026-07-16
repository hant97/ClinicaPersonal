import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InventoryService, Supply } from '../../../core/services/inventory.service';
import { InventoryFormComponent } from '../inventory-form/inventory-form.component';
import { PaginationComponent } from '../../../shared/components/pagination/pagination.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Search, Edit, Trash2, Plus, AlertTriangle } from 'lucide-angular';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';

@Component({
  selector: 'app-inventory-list',
  standalone: true,
  imports: [CommonModule, InventoryFormComponent, LucideAngularModule, PaginationComponent],
  templateUrl: './inventory-list.component.html',
  styleUrls: ['./inventory-list.component.css']
})
export class InventoryListComponent implements OnInit, OnDestroy {
  readonly Search = Search;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Plus = Plus;
  readonly AlertTriangle = AlertTriangle;

  filteredSupplies: Supply[] = [];
  searchTerm: string = '';
  showModal = false;
  selectedSupplyId: number | null = null;
  
  currentPage: number = 0;
  pageSize: number = 10;
  totalPages: number = 0;
  totalElements: number = 0;
  
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
    this.inventoryService.getAllSupplies(this.searchTerm, this.currentPage, this.pageSize).subscribe(page => {
      this.filteredSupplies = page.content;
      this.totalPages = page.page.totalPages;
      this.totalElements = page.page.totalElements;
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
