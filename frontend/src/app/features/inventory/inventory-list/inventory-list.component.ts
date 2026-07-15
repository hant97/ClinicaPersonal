import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { InventoryService, Supply } from '../../../core/services/inventory.service';
import { InventoryFormComponent } from '../inventory-form/inventory-form.component';
import { ToastService } from '../../../shared/services/toast/toast.service';
import { NotificationService } from '../../../shared/services/notification/notification.service';
import { LucideAngularModule, Search, Edit, Trash2, Plus, AlertTriangle } from 'lucide-angular';

@Component({
  selector: 'app-inventory-list',
  standalone: true,
  imports: [CommonModule, InventoryFormComponent, LucideAngularModule],
  templateUrl: './inventory-list.component.html',
  styleUrls: ['./inventory-list.component.css']
})
export class InventoryListComponent implements OnInit {
  readonly Search = Search;
  readonly Edit = Edit;
  readonly Trash2 = Trash2;
  readonly Plus = Plus;
  readonly AlertTriangle = AlertTriangle;

  supplies: Supply[] = [];
  filteredSupplies: Supply[] = [];
  searchTerm: string = '';
  showModal = false;
  selectedSupplyId: number | null = null;

  constructor(
    private inventoryService: InventoryService,
    private toastService: ToastService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadSupplies();
  }

  loadSupplies(): void {
    this.inventoryService.getAllSupplies().subscribe(data => {
      this.supplies = data;
      this.filterSupplies();
    });
  }

  onSearch(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchTerm = target.value.toLowerCase();
    this.filterSupplies();
  }

  filterSupplies(): void {
    if (!this.searchTerm) {
      this.filteredSupplies = [...this.supplies];
    } else {
      this.filteredSupplies = this.supplies.filter(supply => {
        return supply.name.toLowerCase().includes(this.searchTerm);
      });
    }
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
