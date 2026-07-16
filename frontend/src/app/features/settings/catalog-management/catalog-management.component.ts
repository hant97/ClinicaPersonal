import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CatalogService } from '../../../core/services/catalog.service';
import { Catalog, CatalogItem } from '../../../core/models/catalog.model';
import { NotificationService } from '../../../shared/services/notification/notification.service';

@Component({
  selector: 'app-catalog-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalog-management.component.html',
  styleUrls: ['./catalog-management.component.css']
})
export class CatalogManagementComponent implements OnInit {
  catalogs: Catalog[] = [];
  selectedCatalog: Catalog | null = null;
  items: CatalogItem[] = [];
  
  newItemName: string = '';
  newItemCode: string = '';

  constructor(
    private catalogService: CatalogService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadCatalogs();
  }

  loadCatalogs(): void {
    this.catalogService.getAllCatalogs().subscribe({
      next: (data) => {
        this.catalogs = data;
      },
      error: (err) => {
        this.notificationService.alert('Error', 'Error al cargar catálogos', 'error');
      }
    });
  }

  selectCatalog(catalog: Catalog): void {
    this.selectedCatalog = catalog;
    this.items = catalog.items || [];
    this.items.sort((a, b) => a.orderIndex - b.orderIndex);
  }

  addItem(): void {
    if (!this.selectedCatalog || !this.newItemName.trim() || !this.newItemCode.trim()) {
      return;
    }

    const newItem: CatalogItem = {
      itemCode: this.newItemCode.toUpperCase().replace(/\s+/g, '_'),
      itemName: this.newItemName,
      isActive: true,
      orderIndex: this.items.length
    };

    this.catalogService.addCatalogItem(this.selectedCatalog.code, newItem).subscribe({
      next: (savedItem) => {
        this.items.push(savedItem);
        this.newItemName = '';
        this.newItemCode = '';
        this.notificationService.alert('Éxito', 'Ítem agregado exitosamente', 'success');
      },
      error: (err) => {
        console.error(err);
        this.notificationService.alert('Error', 'Error al agregar ítem', 'error');
      }
    });
  }

  toggleActive(item: CatalogItem): void {
    // Como usamos ngModel, item.isActive ya tiene el nuevo valor deseado.
    const updatedItem = { ...item };
    this.catalogService.updateCatalogItem(item.id!, updatedItem).subscribe({
      next: (saved) => {
        item.isActive = saved.isActive;
        this.notificationService.alert('Éxito', `Ítem ${item.isActive ? 'activado' : 'desactivado'}`, 'success');
      },
      error: (err) => {
        console.error(err);
        // Si hay error, revertimos el estado en la interfaz
        item.isActive = !item.isActive;
        this.notificationService.alert('Error', 'Error al actualizar ítem', 'error');
      }
    });
  }
}
